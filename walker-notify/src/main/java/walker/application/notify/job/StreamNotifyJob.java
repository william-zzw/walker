package walker.application.notify.job;

import static walker.application.notify.CoordinatorConst.NOTIFY_SUCCESS_CODE;
import static walker.application.notify.CoordinatorConst.NOTIFY_SUCCESS_KEY;
import static walker.application.notify.CoordinatorConst.NotifyStatus.*;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.github.pagehelper.PageHelper;

import lombok.extern.slf4j.Slf4j;
import walker.application.notify.CoordinatorConst;
import walker.application.notify.config.schedule.NotifyScheduleConst;
import walker.application.notify.entity.WalkerNotify;
import walker.application.notify.entity.WalkerNotifyExample;
import walker.application.notify.entity.WalkerTransaction;
import walker.application.notify.entity.WalkerTransactionExample;
import walker.application.notify.mapper.WalkerNotifyMapper;
import walker.application.notify.mapper.WalkerTransactionMapper;
import walker.common.util.Utility;

/**
 * Copyright: Copyright (C) github.com/devpage, Inc. All rights reserved.
 * <p>
 *
 * @author SONG
 * @since 2018/11/3 0:46
 */
@Slf4j
public class StreamNotifyJob implements DataflowJob<WalkerNotify> {

    @Resource
    private WalkerTransactionMapper walkerTransactionMapper;

    @Resource
    private WalkerNotifyMapper walkerNotifyMapper;

    private RestTemplate restTemplate = new RestTemplate();

    private int[] NOTIFY_TYPE =
        new int[] {CoordinatorConst.NotifyType.COMMIT.ordinal(), CoordinatorConst.NotifyType.CANCEL.ordinal()};

    /**
     * 总的分片数 要等于 分表的数量
     * 
     * @param shardingContext
     * @return
     */
    @Override
    public List<WalkerNotify> fetchData(ShardingContext shardingContext) {
        int notifyType = NOTIFY_TYPE[shardingContext.getShardingItem() % NOTIFY_TYPE.length];
        log.info("------Thread ID: {}, 任务总片数: {}, 当前第{}分片, NOTIFY_TYPE :{}", Thread.currentThread().getId(),
            shardingContext.getShardingTotalCount(), shardingContext.getShardingItem(), notifyType);
        sleep(NotifyScheduleConst.INTERNAL_SLEEP_MICROSECONDS);

        long createTimeBeginFilter =
            Utility.unix_timestamp() - CoordinatorConst.PROCESS_RECENT_DAY * CoordinatorConst.SECONDS_OF_DAY;

        PageHelper.startPage(1, NotifyScheduleConst.INTERNAL_NOTIFY_FETCH_SIZE);
        WalkerNotifyExample waiteToNotifyExample = new WalkerNotifyExample();
        waiteToNotifyExample.createCriteria().andGmtCreateGreaterThanOrEqualTo(createTimeBeginFilter)
            .andNotifyTypeEqualTo(notifyType).andNotifyStatusEqualTo(WAITING_EXECUTE);
        return walkerNotifyMapper.selectIndexedTableByExample(shardingContext.getShardingItem(), waiteToNotifyExample);
    }

    @Override
    public void processData(ShardingContext shardingContext, List<WalkerNotify> data) {
        if (!CollectionUtils.isEmpty(data)) {
            for (WalkerNotify notify : data) {
                // 应当从redis中获取notify是notifyStatus
                int redisCachedNotifyStatus = notify.getNotifyStatus();
                boolean notifyHasNoLocker = redisCachedNotifyStatus != NOTIFYING;
                if (!notifyHasNoLocker) {
                    log.info(" execute redis.lock({}), lock condition must use global id", notify.getId());
                    process(notify, shardingContext);
                }
            }
        }
    }

    public void process(WalkerNotify notify, ShardingContext shardingContext) {
        int notifyTaskTableIndex = shardingContext.getShardingItem();
        int notifyType = NOTIFY_TYPE[shardingContext.getShardingItem() % NOTIFY_TYPE.length];


        String notifyLockId = ("redis.notify.lock(" + notify.getId() + ")");
        if (StringUtils.isNotEmpty(notifyLockId)) {
            // get notify lock
            try {
                WalkerNotify updateEntity = new WalkerNotify();
                updateEntity.setId(notify.getId());

                Map<String, Object> notifyResponse = doNotify(notify);
                // todo append callback string to url
                log.info("doNotify masterGid:{}, branchGFid:{} URL:{}, BODY:{}, response:{}", notify.getMasterGid(),
                    notify.getBranchGid(), notify.getNotifyUrl(), notify.getNotifyBody(), notifyResponse);
                if (notifyResponse != null) {
                    String returnCode = (String)notifyResponse.get(NOTIFY_SUCCESS_KEY);
                    if (StringUtils.isNoneEmpty(returnCode)) {
                        if (returnCode.equals(NOTIFY_SUCCESS_CODE)) {
                            updateEntity.setNotifyStatus(NOTIFY_SUCCESS);

                            Integer updateTransactionTxStatus = null;
                            if (notifyType == CoordinatorConst.NotifyType.COMMIT.ordinal()) {
                                updateTransactionTxStatus = CoordinatorConst.TransactionTxStatus.COMMITED;
                            } else if (notifyType == CoordinatorConst.NotifyType.CANCEL.ordinal()) {
                                updateTransactionTxStatus = CoordinatorConst.TransactionTxStatus.CANCELED;
                            }
                            updateWalkerTransactionTxStatus(notify, updateTransactionTxStatus);
                        } else {
                            if (notify.getRetryNum() > CoordinatorConst.NOTIFY_RETRY_MAX) {
                                // todo think if notify failure, how to process transaction row txStatus
                                updateEntity.setNotifyStatus(NOTIFY_FAILURE);
                            } else {
                                updateEntity.setRetryNum(notify.getRetryNum() + 1);
                            }
                        }
                    } else {
                        log.info(
                            "doNotify masterGid:{}, branchGFid:{} response not contains key returnCode, will retry");
                        updateEntity.setRetryNum(notify.getRetryNum() + 1);
                    }
                } else {
                    log.info("doNotify response empty, will retry");
                    updateEntity.setRetryNum(notify.getRetryNum() + 1);
                }
                walkerNotifyMapper.updateIndexedTableByPrimaryKeySelective(shardingContext.getShardingItem(),
                    updateEntity);
            } catch (Exception e) {
                log.error("notifyJob process error", e);
            } finally {
                log.info("redis.notify.unlock({})", notifyLockId);
            }
        }
    }

    @Transactional
    public Map<String, Object> doNotify(WalkerNotify notify) {
        return restTemplate.postForObject(notify.getNotifyUrl(), notify.getNotifyBody(), Map.class);
    }

    private int getNotifyTransactionTableIndex(WalkerNotify notify) {
        return System.identityHashCode(notify.getMasterGid()) % CoordinatorConst.TRANSACTION_SHARDING_COUNT;
    }

    private int updateWalkerTransactionTxStatus(WalkerNotify notify, Integer updateTxStatus) {
        int targetTransactionTableIndex = getNotifyTransactionTableIndex(notify);
        WalkerTransaction transaction = new WalkerTransaction();
        transaction.setTxStatus(updateTxStatus);
        transaction.setGmtModified(Utility.unix_timestamp());
        WalkerTransactionExample transactionExample = new WalkerTransactionExample();
        transactionExample.createCriteria().andAppIdEqualTo(notify.getAppId()).andMasterGidEqualTo(notify.getMasterGid()).andBranchGidEqualTo(notify.getBranchGid());
        return walkerTransactionMapper.updateIndexedTableByExampleSelective(targetTransactionTableIndex, transaction, transactionExample);
    }

    void sleep(int i) {
        try {
            Thread.sleep(i);
        } catch (InterruptedException e) {
            log.error("{} interrupted", getClass().getName(), e);
        }
    }
}

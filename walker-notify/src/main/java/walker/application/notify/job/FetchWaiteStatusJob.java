package walker.application.notify.job;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

import static walker.application.notify.CoordinatorConst.NOTIFY_SHARDING_COUNT;

/**
 * Copyright: Copyright (C) github.com/devpage, Inc. All rights reserved.
 * <p>
 *
 * @author SONG
 * @since 2018/11/3 0:46
 */
@Slf4j
public class FetchWaiteStatusJob implements DataflowJob<WalkerTransaction> {

    @Resource
    private WalkerTransactionMapper walkerTransactionMapper;

    @Resource
    private WalkerNotifyMapper walkerNotifyMapper;

    private int[] EXECUTE_TX_STATUS = new int[] {
            CoordinatorConst.TransactionTxStatus.WAITE_COMMIT,
            CoordinatorConst.TransactionTxStatus.WAITE_CANCEL
    };

    /**
     * 总的分片数 要等于 分表的数量
     * 
     * @param shardingContext
     * @return
     */
    @Override
    public List<WalkerTransaction> fetchData(ShardingContext shardingContext) {
        try {
            Thread.sleep(NotifyScheduleConst.INTERNAL_SLEEP_MICROSECONDS);
        } catch (InterruptedException e) {
            log.error("walker.application.notify.job.FetchWaiteStatusJob.fetchData interrupted", e);
        }
        int executeTxStatus = EXECUTE_TX_STATUS[shardingContext.getShardingItem() % EXECUTE_TX_STATUS.length];
        log.info("------Thread ID: {}, 任务总片数: {}, 当前第{}分片, FETCH TYPE:{}", Thread.currentThread().getId(),
            shardingContext.getShardingTotalCount(), shardingContext.getShardingItem(), executeTxStatus);

        long createTimeBeginFilter =
            Utility.unix_timestamp() - CoordinatorConst.PROCESS_RECENT_DAY * CoordinatorConst.SECONDS_OF_DAY;

        PageHelper.startPage(1, NotifyScheduleConst.INTERNAL_MASTER_FETCH_WAITE_STATUS_SIZE);
        WalkerTransactionExample waiteToExecuteExample = new WalkerTransactionExample();
        waiteToExecuteExample.createCriteria().andGmtCreateGreaterThanOrEqualTo(createTimeBeginFilter)
            .andTxStatusEqualTo(executeTxStatus);
        return walkerTransactionMapper.selectIndexedTableByExample(shardingContext.getShardingItem(),
            waiteToExecuteExample);
    }

    @Override
    public void processData(ShardingContext shardingContext, List<WalkerTransaction> data) {
        if (!CollectionUtils.isEmpty(data)) {
            int tableIndex = shardingContext.getShardingItem();
            int executeTxStatus = EXECUTE_TX_STATUS[shardingContext.getShardingItem() % EXECUTE_TX_STATUS.length];
            int targetTxStatus  = CoordinatorConst.TransactionTxStatus.COMMITTING;
            if (executeTxStatus == CoordinatorConst.TransactionTxStatus.WAITE_COMMIT) {
                targetTxStatus  = CoordinatorConst.TransactionTxStatus.COMMITTING;
            } else if (executeTxStatus == CoordinatorConst.TransactionTxStatus.WAITE_CANCEL){
                targetTxStatus  = CoordinatorConst.TransactionTxStatus.CANCELING;
            }
            for (WalkerTransaction walkerTransaction : data) {
                process(walkerTransaction, tableIndex, targetTxStatus);
            }
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    public void process(WalkerTransaction walkerTransaction, int walkerTransactionTableIndex, Integer targetTxStatus) {
        WalkerNotifyExample notifyExample = new WalkerNotifyExample();
        notifyExample.createCriteria()
                .andAppIdEqualTo(walkerTransaction.getAppId())
                .andMasterGidEqualTo(walkerTransaction.getMasterGid())
                .andBranchGidEqualTo(walkerTransaction.getBranchGid());

        WalkerNotify updateNotify = new WalkerNotify();
        updateNotify.setGmtModified(Utility.unix_timestamp());
        updateNotify.setNotifyStatus(CoordinatorConst.NotifyStatus.WAITING_EXECUTE);

        int notifyTableIndex = System.identityHashCode(walkerTransaction.getBranchGid()) % NOTIFY_SHARDING_COUNT;
        walkerNotifyMapper.updateIndexedTableByExampleSelective(notifyTableIndex, updateNotify, notifyExample);

        WalkerTransaction updateTransaction = new WalkerTransaction();
        updateTransaction.setTxStatus(targetTxStatus);
        updateTransaction.setId(walkerTransaction.getId());
        walkerTransactionMapper.updateIndexedTableByPrimaryKeySelective(walkerTransactionTableIndex, updateTransaction);
    }
}

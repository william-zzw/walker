package walker.application.coordinator.service.impl;

import static walker.application.coordinator.CoordinatorConst.TransactionTxStatus.*;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import walker.application.coordinator.CoordinatorConst;
import walker.application.coordinator.entity.WalkerNotify;
import walker.application.coordinator.entity.WalkerNotifyExample;
import walker.application.coordinator.entity.WalkerTransaction;
import walker.application.coordinator.entity.WalkerTransactionExample;
import walker.application.coordinator.mapper.WalkerNotifyMapper;
import walker.application.coordinator.mapper.WalkerTransactionMapper;
import walker.application.coordinator.service.WalkerTransactionService;
import walker.common.util.Utility;
import walker.protocol.message.command.TransactionCommand;

/**
 * @author SONG
 */
@Service
@Slf4j
public class WalkerTransactionServiceImpl implements WalkerTransactionService {

    @Resource
    private WalkerTransactionMapper walkerTransactionMapper;

    @Resource
    private WalkerNotifyMapper walkerNotifyMapper;

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int publish(WalkerTransaction transaction) {
        return walkerTransactionMapper.insertSelective(transaction);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateSingleStatusToPrepareCommit(TransactionCommand.TransactionCommit toCommand) {
        String appId = toCommand.getAppId();
        String masterGid = toCommand.getMasterGid();
        String branchGid = toCommand.getBranchGid();
        updateSingleStatusToWaite(appId, masterGid, branchGid, WAITE_COMMIT);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void startWalkerTransactionCommitProcess(TransactionCommand.TransactionCommit toCommand) {
        String appId = toCommand.getAppId();
        String masterGid = toCommand.getMasterGid();
        final long unixTimestamp = Utility.unix_timestamp();
        // section
        WalkerTransactionExample example = new WalkerTransactionExample();
        example.createCriteria().andAppIdEqualTo(appId).andMasterGidEqualTo(masterGid);
        // update content
        WalkerTransaction target = new WalkerTransaction();
        target.setGmtModified(unixTimestamp);
        target.setTxStatus(WAITE_COMMIT);
        // doUpdate
        walkerTransactionMapper.updateByExampleSelective(target, example);

        // 开始更新所有的notify
        List<WalkerTransaction> transactionEntityList = walkerTransactionMapper.selectByExample(example);
        long createTimeBeginFilter =
            Utility.unix_timestamp() - CoordinatorConst.PROCESS_RECENT_DAY * CoordinatorConst.SECONDS_OF_DAY;

        final int notifyType = CoordinatorConst.NotifyType.COMMIT.ordinal();

        final WalkerNotify notifyTemplate = new WalkerNotify();
        notifyTemplate.setNotifyStatus(CoordinatorConst.NotifyStatus.WAITING_EXECUTE);
        notifyTemplate.setGmtModified(unixTimestamp);

        transactionEntityList.forEach(branch -> {
            WalkerNotifyExample notifyExample = new WalkerNotifyExample();
            notifyExample.createCriteria().
                    andGmtCreateGreaterThanOrEqualTo(createTimeBeginFilter)
                    .andAppIdEqualTo(branch.getAppId())
                    .andMasterGidEqualTo(branch.getMasterGid())
                    .andBranchGidEqualTo(branch.getBranchGid())
                    .andNotifyTypeEqualTo(notifyType);

            walkerNotifyMapper.updateByExample(notifyTemplate, notifyExample);
        });
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void updateSingleStatusToPrepareCancel(TransactionCommand.TransactionBroken toCommand) {
        String appId = toCommand.getAppId();
        String masterGid = toCommand.getMasterGid();
        String branchGid = toCommand.getBranchGid();
        updateSingleStatusToWaite(appId, masterGid, branchGid, WAITE_CANCEL);
    }

    /**
     * 更新分支事务的状态
     * @param appId
     * @param masterGid
     * @param branchGid
     * @param waiteStatus
     */
    private void updateSingleStatusToWaite(final String appId, final String masterGid, final String branchGid, final int waiteStatus) {
        // section
        WalkerTransactionExample example = new WalkerTransactionExample();
        example.createCriteria().andAppIdEqualTo(appId).andMasterGidEqualTo(masterGid).andBranchGidEqualTo(branchGid);
        // update content
        WalkerTransaction target = new WalkerTransaction();
        target.setGmtModified(Utility.unix_timestamp());
        target.setTxStatus(waiteStatus);
        // doUpdate
        walkerTransactionMapper.updateByExampleSelective(target, example);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public void startWalkerTransactionCancelProcess(TransactionCommand.TransactionBroken toCommand) {
        String appId = toCommand.getAppId();
        String masterGid = toCommand.getMasterGid();
        final long unixTimestamp = Utility.unix_timestamp();
        // section
        WalkerTransactionExample example = new WalkerTransactionExample();
        example.createCriteria().andAppIdEqualTo(appId).andMasterGidEqualTo(masterGid);
        // update content
        WalkerTransaction target = new WalkerTransaction();
        target.setGmtModified(unixTimestamp);
        target.setTxStatus(WAITE_CANCEL);
        // doUpdate
        walkerTransactionMapper.updateByExampleSelective(target, example);

        // 开始更新所有的notify
        List<WalkerTransaction> transactionEntityList = walkerTransactionMapper.selectByExample(example);
        long createTimeBeginFilter =
                Utility.unix_timestamp() - CoordinatorConst.PROCESS_RECENT_DAY * CoordinatorConst.SECONDS_OF_DAY;

        final int notifyType = CoordinatorConst.NotifyType.CANCEL.ordinal();

        final WalkerNotify notifyTemplate = new WalkerNotify();
        notifyTemplate.setNotifyStatus(CoordinatorConst.NotifyStatus.WAITING_EXECUTE);
        notifyTemplate.setGmtModified(unixTimestamp);

        transactionEntityList.forEach(branch -> {
            WalkerNotifyExample notifyExample = new WalkerNotifyExample();
            notifyExample.createCriteria().
                    andGmtCreateGreaterThanOrEqualTo(createTimeBeginFilter)
                    .andAppIdEqualTo(branch.getAppId())
                    .andMasterGidEqualTo(branch.getMasterGid())
                    .andBranchGidEqualTo(branch.getBranchGid())
                    .andNotifyTypeEqualTo(notifyType);

            walkerNotifyMapper.updateByExample(notifyTemplate, notifyExample);
        });
    }
}

package walker.application.coordinator.service;

import walker.application.coordinator.entity.WalkerTransaction;
import walker.protocol.message.command.TransactionCommand;

/**
 * @author SONG
 */
public interface WalkerTransactionService {

    /**
     * 声明一个事务
     * @param transaction
     * @return
     */
    int publish(WalkerTransaction transaction);

    /**
     * 更新分支事务的状态为待提交
     * @param toCommand
     */
    void updateSingleStatusToPrepareCommit(TransactionCommand.TransactionCommit toCommand);

    /**开始了 commit 流程
     * @param toCommand
     */
    void startWalkerTransactionCommitProcess(TransactionCommand.TransactionCommit toCommand);

    /**
     * 更新分支事务的状态为待取消
     * @param toCommand
     */
    void updateSingleStatusToPrepareCancel(TransactionCommand.TransactionBroken toCommand);

    /**
     * 开始了 cancel 流程
     * @param toCommand
     */
    void startWalkerTransactionCancelProcess(TransactionCommand.TransactionBroken toCommand);
}

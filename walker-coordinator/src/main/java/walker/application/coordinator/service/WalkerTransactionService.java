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

    void postCommit(TransactionCommand.TransactionCommit toCommand);

    void postCancel(TransactionCommand.TransactionBroken toCommand);
}

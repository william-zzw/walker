package walker.application.coordinator.service;

import walker.protocol.message.command.TransactionCommand;

public interface WalkerTransactionService {

    int add(WalkerTransaction transaction);

    void doCommit(TransactionCommand.TransactionCommit toCommand);

    void doBroken(TransactionCommand.TransactionBroken toCommand);
}

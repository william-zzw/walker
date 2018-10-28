package walker.core.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import walker.common.context.WalkerContext;
import walker.protocol.compensate.Participant;
import walker.protocol.message.WalkerMessage;
import walker.protocol.message.command.CompensateCommand;
import walker.protocol.message.command.TransactionCommand;
import walker.rabbitmq.WalkerMessageUtils;
import walker.rabbitmq.WalkerRabbitTemplate;

@Component
@EnableConfigurationProperties(WalkerProxyProperties.class)
@ConditionalOnClass(WalkerRabbitTemplate.class)
public class WalkerProxy {

    @Autowired
    private WalkerProxyProperties walkerProxyProperties;

    @Autowired
    @Qualifier("walkerRabbitTemplate")
    private WalkerRabbitTemplate walkerRabbitTemplate;

    private static final String ROUTE_TO_REPORT_TRANSACTION_KEY = "ROUTE_TO_REPORT_TRANSACTION";
    private static final String ROUTE_TO_REPORT_COMPENSATE_KEY = "ROUTE_TO_REPORT_COMPENSATE";


    private String appId(){
        return walkerProxyProperties.getAppId();
    }

    public void transactionStart(WalkerContext walkerContext, boolean declare) {
        TransactionCommand.TransactionPublish command = new TransactionCommand.TransactionPublish();
        command.setAppId(appId()).setMasterGid(walkerContext.getMasterGid()).setBranchGid(getCurrentBranchGid(walkerContext)).setDeclare(declare);
        WalkerMessage walkerMessage = WalkerMessageUtils.toWalkerMessage(TransactionCommand.PUBLISH, command);
        walkerRabbitTemplate.send(ROUTE_TO_REPORT_TRANSACTION_KEY, walkerMessage);
    }

    public void transactionBroken(WalkerContext walkerContext, Throwable ex) {
        TransactionCommand.TransactionBroken command = new TransactionCommand.TransactionBroken();
        command.setAppId(appId()).setMasterGid(walkerContext.getMasterGid()).setBranchGid(getCurrentBranchGid(walkerContext)).setException(ex.getMessage());
        WalkerMessage walkerMessage = WalkerMessageUtils.toWalkerMessage(TransactionCommand.BROKEN, command);
        walkerRabbitTemplate.send(ROUTE_TO_REPORT_TRANSACTION_KEY, walkerMessage);
    }

    public void transactionCommit(WalkerContext walkerContext) {
        TransactionCommand.TransactionCommit command = new TransactionCommand.TransactionCommit();
        command.setAppId(appId()).setMasterGid(walkerContext.getMasterGid()).setBranchGid(getCurrentBranchGid(walkerContext));
        WalkerMessage walkerMessage = WalkerMessageUtils.toWalkerMessage(TransactionCommand.COMMIT, command);
        walkerRabbitTemplate.send(ROUTE_TO_REPORT_TRANSACTION_KEY, walkerMessage);
    }

    public void compensateCommit(WalkerContext walkerContext, Participant commitParticipant, Participant cancelParticipant) {
        CompensateCommand.CompensateCommit command = new CompensateCommand.CompensateCommit();
        command.setAppId(appId()).setMasterGid(walkerContext.getMasterGid()).setBranchGid(getCurrentBranchGid(walkerContext)).setCommitParticipant(commitParticipant).setCancelParticipant(cancelParticipant);
        WalkerMessage walkerMessage = WalkerMessageUtils.toWalkerMessage(CompensateCommand.COMMIT, command);
        walkerRabbitTemplate.send(ROUTE_TO_REPORT_COMPENSATE_KEY, walkerMessage);
    }

    public void compensateBroken(WalkerContext walkerContext, Throwable ex) {
        CompensateCommand.CompensateBroken command = new CompensateCommand.CompensateBroken();
        command.setAppId(appId()).setMasterGid(walkerContext.getMasterGid()).setBranchGid(getCurrentBranchGid(walkerContext)).setException(ex.getMessage());
        WalkerMessage walkerMessage = WalkerMessageUtils.toWalkerMessage(CompensateCommand.BROKEN, command);
        walkerRabbitTemplate.send(ROUTE_TO_REPORT_COMPENSATE_KEY, walkerMessage);
    }
    // 如果补偿失败，但是业务都成功?? 所以要避免这种现象

    private String getCurrentBranchGid(WalkerContext walkerContext) {
        String currentBranchGid = walkerContext.getBranchGids().get(walkerContext.getDepth().intValue());
        return currentBranchGid;
    }
}
package walker.application.coordinator.rabbitmq.consumer;

import static walker.application.coordinator.CoordinatorConst.TransactionTxStatus.*;

import java.time.Instant;

import javax.annotation.Resource;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
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
import walker.common.util.GsonUtils;
import walker.protocol.compensate.Participant;
import walker.protocol.message.RabbitConst;
import walker.protocol.message.WalkerMessage;
import walker.protocol.message.command.CompensateCommand;
import walker.rabbitmq.WalkerMessageUtils;

/**
 * @author SONG
 */
@Service
@Slf4j
public class CompensateCommandConsumer {

    @Resource
    private WalkerNotifyMapper walkerNotifyMapper;

    @Resource
    private WalkerTransactionMapper walkerTransactionMapper;

    @RabbitListener(
            admin = RabbitConst.RABBITMQ_ADMIN_NAME,
            bindings = @QueueBinding(
                    value = @Queue(value = RabbitConst.WALKER_COMPENSATE_QUEUE, durable = "true", autoDelete = "false", exclusive = "false"),
                    exchange = @Exchange(value = RabbitConst.EXCHANGE_NAME, durable = "true", type = RabbitConst.EXCHANGE_TYPE_TOPIC),
                    key = RabbitConst.ROUTE_TO_REPORT_COMPENSATE)
    )
    public void handlerAmqpMessage(Message message, com.rabbitmq.client.Channel channel) throws Exception {
        WalkerMessage walkerMessage = WalkerMessageUtils.fromAmqpMessage(message);
        if (walkerMessage != null) {
            switch (walkerMessage.getCommand()) {
                case CompensateCommand.COMMIT:
                    handleCommit(WalkerMessageUtils.toCommand(walkerMessage, CompensateCommand.CompensateCommit.class));
                    break;
                case CompensateCommand.BROKEN:
                    handleBroken(WalkerMessageUtils.toCommand(walkerMessage, CompensateCommand.CompensateBroken.class));
                    break;
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @Async
    @Transactional(rollbackFor = {Exception.class})
    public void handleCommit(CompensateCommand.CompensateCommit command) {
        Instant now = Instant.now();
        long unixTimestamp = now.getEpochSecond();
        WalkerNotify commitNotify = recovery(unixTimestamp, command, CoordinatorConst.NotifyType.COMMIT);
        WalkerNotify rollbackNotify = recovery(unixTimestamp, command, CoordinatorConst.NotifyType.CANCEL);
        walkerNotifyMapper.insertSelective(commitNotify);
        walkerNotifyMapper.insertSelective(rollbackNotify);
    }

    @Async
    @Transactional(rollbackFor = {Exception.class})
    public void handleBroken(CompensateCommand.CompensateBroken command) {
        Instant now = Instant.now();
        long unixTimestamp = now.getEpochSecond();
        // just update transaction TxStatus to WAITE_ROLLBACK
        markTxStatusBroken(unixTimestamp, command);
        // active NotifyType.ROLLBACK notify_status to WAITING_EXECUTE
        prepareNotifyCancel(unixTimestamp, command);
    }

    private void markTxStatusBroken(long unixTimestamp, CompensateCommand.CompensateBroken command) {
        log.info("get broken report , reporter appId={}, masterGid={}, branchGid={}, cause={}", command.getAppId(), command.getMasterGid(), command.getBranchGid(), command.getException());
        WalkerTransactionExample example = new WalkerTransactionExample();
        example.createCriteria().andAppIdEqualTo(command.getAppId()).andMasterGidEqualTo(command.getMasterGid());
        WalkerTransaction target = new WalkerTransaction();
        target.setTxStatus(WAITE_CANCEL);
        target.setGmtModified(unixTimestamp);
        walkerTransactionMapper.updateByExampleSelective(target, example);
    }

    private void prepareNotifyCancel(long unixTimestamp, CompensateCommand.CompensateBroken command) {
        WalkerNotifyExample example = new WalkerNotifyExample();
        example.createCriteria()
                .andAppIdEqualTo(command.getAppId())
                .andMasterGidEqualTo(command.getMasterGid())
                .andNotifyTypeEqualTo(CoordinatorConst.NotifyType.CANCEL.ordinal())
                .andNotifyStatusEqualTo(CoordinatorConst.NotifyStatus.RECORDED);
        WalkerNotify target = new WalkerNotify();
        target.setNotifyStatus(CoordinatorConst.NotifyStatus.WAITING_EXECUTE);
        target.setGmtModified(unixTimestamp);
        walkerNotifyMapper.updateByExampleSelective(target, example);
    }

    private WalkerNotify recovery(long unixTimestamp, CompensateCommand.CompensateCommit command, CoordinatorConst.NotifyType notifyType) {
        WalkerNotify notify = new WalkerNotify();
        notify.setGmtCreate(unixTimestamp);
        notify.setGmtModified(unixTimestamp);
        notify.setAppId(command.getAppId());
        notify.setMasterGid(command.getMasterGid());
        notify.setBranchGid(command.getBranchGid());
        notify.setNotifyType(notifyType.ordinal());
        notify.setRetryNum(0);
        notify.setNotifyStatus(CoordinatorConst.NotifyStatus.RECORDED);
        switch (notifyType) {
            case COMMIT:
                Participant commitParticipant =  command.getCommitParticipant();
                notify.setNotifyUrl(commitParticipant.getUrl());
                notify.setNotifyBody(GsonUtils.toJson(commitParticipant.getRequestBody()));
                break;
            case CANCEL:
                Participant cancelParticipant =  command.getCancelParticipant();
                notify.setNotifyUrl(cancelParticipant.getUrl());
                notify.setNotifyBody(GsonUtils.toJson(cancelParticipant.getRequestBody()));
                break;
             default:
                 break;
        }
        return notify;
    }

}

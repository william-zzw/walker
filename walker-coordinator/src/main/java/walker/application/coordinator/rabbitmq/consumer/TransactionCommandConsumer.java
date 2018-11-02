package walker.application.coordinator.rabbitmq.consumer;

import javax.annotation.Resource;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.common.base.Preconditions;

import lombok.extern.slf4j.Slf4j;
import walker.application.coordinator.CoordinatorConst;
import walker.application.coordinator.entity.WalkerTransaction;
import walker.application.coordinator.service.WalkerTransactionService;
import walker.common.util.Utility;
import walker.protocol.message.RabbitConst;
import walker.protocol.message.WalkerMessage;
import walker.protocol.message.command.TransactionCommand;
import walker.rabbitmq.WalkerMessageUtils;

/**
 * https://www.jb51.net/article/131708.htm https://blog.csdn.net/huanghaopeng62/article/details/54618341
 * https://blog.csdn.net/csdnofzhc/article/details/79422858
 * 
 * @author SONG
 */
@Service
@Slf4j
public class TransactionCommandConsumer {

    @Resource
    private WalkerTransactionService walkerTransactionService;

    @RabbitListener(admin = RabbitConst.RABBITMQ_ADMIN_NAME, bindings = @QueueBinding(
        // 1.test.demo.send:队列名,2.true:是否长期有效,3.false:是否自动删除
        value = @Queue(value = RabbitConst.WALKER_TRANSACTION_QUEUE, durable = "true", autoDelete = "false",
            exclusive = "false"),
        // 1.default.topic交换器名称(默认值),2.true:是否长期有效,3.topic:类型是topic
        exchange = @Exchange(value = RabbitConst.EXCHANGE_NAME, durable = "true",
            type = RabbitConst.EXCHANGE_TYPE_TOPIC),
        // test2.send:路由的名称,ProducerConfig 里面 绑定的路由名称(xxxx.to(exchange).with("test2.send")))
        key = RabbitConst.ROUTE_TO_REPORT_TRANSACTION))
    public void handlerAmqpMessage(Message message, com.rabbitmq.client.Channel channel) throws Exception {
        WalkerMessage walkerMessage = WalkerMessageUtils.fromAmqpMessage(message);
        if (walkerMessage != null) {
            switch (walkerMessage.getCommand()) {
                case TransactionCommand.PUBLISH:
                    handlePublish(
                        WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionPublish.class));
                    break;
                case TransactionCommand.COMMIT:
                    handleCommit(
                        WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionCommit.class));
                    break;
                case TransactionCommand.BROKEN:
                    handleBroken(
                        WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionBroken.class));
                    break;
                default:
                    break;
            }
            // false只确认当前一个消息收到，true确认所有consumer获得的消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    @Async
    public void handlePublish(TransactionCommand.TransactionPublish toCommand) {
        WalkerTransaction walkerTransaction = new WalkerTransaction();
        walkerTransaction.setAppId(toCommand.getAppId());
        walkerTransaction.setMasterGid(toCommand.getMasterGid());
        walkerTransaction.setBranchGid(toCommand.getBranchGid());
        walkerTransaction.setIsDeclare(false);
        long timestamp = Utility.unix_timestamp();
        walkerTransaction.setGmtCreate(timestamp);
        walkerTransaction.setGmtModified(timestamp);
        walkerTransaction.setTxStatus(CoordinatorConst.TransactionTxStatus.RECORDED);
        walkerTransactionService.publish(walkerTransaction);
    }

    /**
     * 万一这里把 commit 消息丢了呢？
     *
     * 所以master 在提交的时候, 本地要加一个log_transaction_statement事务状态记录表, 提交到该表的记录说明都是已启动了 提交全局事务的
     *
     * @param toCommand
     */
    @Async
    public void handleCommit(TransactionCommand.TransactionCommit toCommand) {
        String appId = toCommand.getAppId();
        Preconditions.checkNotNull(appId, "process commit command ,appId can't be null");
        String masterGid = toCommand.getMasterGid();
        Preconditions.checkNotNull(masterGid, "process commit command ,masterGid can't be null");
        String branchGid = toCommand.getBranchGid();
        Preconditions.checkNotNull(branchGid, "process commit command ,branchGid can't be null");
        if (masterGid.equals(branchGid)) {
            log.info("[主事务提交], appId:{}, masterGid:{}", toCommand.getAppId(), toCommand.getMasterGid());
            walkerTransactionService.startWalkerTransactionCommitProcess(toCommand);
        } else {
            log.info("[分支事务提交], appId:{}, masterGid:{}, branchGid:{}", toCommand.getAppId(), toCommand.getMasterGid(), toCommand.getBranchGid());
            walkerTransactionService.updateSingleStatusToPrepareCommit(toCommand);
        }
    }

    @Async
    public void handleBroken(TransactionCommand.TransactionBroken toCommand) {
        String appId = toCommand.getAppId();
        Preconditions.checkNotNull(appId, "process cancel command ,appId can't be null");
        String masterGid = toCommand.getMasterGid();
        Preconditions.checkNotNull(masterGid, "process cancel command ,masterGid can't be null");
        String branchGid = toCommand.getBranchGid();
        Preconditions.checkNotNull(branchGid, "process cancel command ,branchGid can't be null");

        if (!masterGid.equals(branchGid)) {
            walkerTransactionService.updateSingleStatusToPrepareCancel(toCommand);
        }
        walkerTransactionService.startWalkerTransactionCancelProcess(toCommand);
    }

}

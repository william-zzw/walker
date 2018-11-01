package walker.application.coordinator.rabbitmq.consumer;

import javax.annotation.Resource;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
                case TransactionCommand.BROKEN:
                    handleBroken(
                        WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionBroken.class));
                    break;
                case TransactionCommand.COMMIT:
                    handleCommit(
                        WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionCommit.class));
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
        int timestamp = Utility.getTimestamp();
        walkerTransaction.setGmtCreate(timestamp);
        walkerTransaction.setGmtModified(timestamp);
        walkerTransaction.setTxStatus(CoordinatorConst.TransactionTxStatus.RECORDED);
        walkerTransactionService.publish(walkerTransaction);
    }

    @Async
    public void handleCommit(TransactionCommand.TransactionCommit toCommand) {
        walkerTransactionService.postCommit(toCommand);
    }

    @Async
    public void handleBroken(TransactionCommand.TransactionBroken toCommand) {
        walkerTransactionService.postCancel(toCommand);
    }

}

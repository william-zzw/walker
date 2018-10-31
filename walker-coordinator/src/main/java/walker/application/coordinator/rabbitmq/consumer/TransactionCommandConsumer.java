package walker.application.coordinator.rabbitmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import walker.application.coordinator.CoordinatorConst;
import walker.application.coordinator.service.WalkerTransactionService;
import walker.common.util.Utility;
import walker.protocol.message.RabbitConst;
import walker.protocol.message.WalkerMessage;
import walker.protocol.message.command.TransactionCommand;
import walker.rabbitmq.WalkerMessageUtils;

import javax.annotation.Resource;

/**
 * https://www.jb51.net/article/131708.htm
 * https://blog.csdn.net/huanghaopeng62/article/details/54618341
 * https://blog.csdn.net/csdnofzhc/article/details/79422858
 */
@Service
@Slf4j
public class TransactionCommandConsumer {

    @Resource
    private WalkerTransactionService walkerTransactionService;

    @RabbitListener(
            admin = RabbitConst.RABBITMQ_ADMIN_NAME,
            bindings = @QueueBinding(
                    //1.test.demo.send:队列名,2.true:是否长期有效,3.false:是否自动删除
                    value = @Queue(value = RabbitConst.WALKER_TRANSACTION_QUEUE, durable = "true", autoDelete = "false", exclusive = "false"),
                    //1.default.topic交换器名称(默认值),2.true:是否长期有效,3.topic:类型是topic
                    exchange = @Exchange(value = RabbitConst.EXCHANGE_NAME, durable = "true", type = RabbitConst.EXCHANGE_TYPE_TOPIC),
                    //test2.send:路由的名称,ProducerConfig 里面 绑定的路由名称(xxxx.to(exchange).with("test2.send")))
                    key = RabbitConst.ROUTE_TO_REPORT_TRANSACTION)
    )
    public void handlerAmqpMessage(Message message, com.rabbitmq.client.Channel channel) throws Exception {
        WalkerMessage walkerMessage = WalkerMessageUtils.fromAmqpMessage(message);
        if (walkerMessage != null) {
            switch (walkerMessage.getCommand()) {
                case TransactionCommand.PUBLISH:
                    handlePublish(WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionPublish.class));
                    break;
                case TransactionCommand.BROKEN:
                    handleBroken(WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionBroken.class));
                    break;
                case TransactionCommand.COMMIT:
                    handleCommit(WalkerMessageUtils.toCommand(walkerMessage, TransactionCommand.TransactionCommit.class));
                    break;
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); // false只确认当前一个消息收到，true确认所有consumer获得的消息
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
        walkerTransaction.setStatus(CoordinatorConst.WalkerTransactionStatus.ADDED);
        walkerTransactionService.add(walkerTransaction);
    }

    @Async
    public void handleCommit(TransactionCommand.TransactionCommit toCommand) {
        walkerTransactionService.doCommit(toCommand);
    }

    @Async
    public void handleBroken(TransactionCommand.TransactionBroken toCommand) {
        walkerTransactionService.doBroken(toCommand);
    }

    /*
        DROP TABLE IF EXISTS `user`;
        CREATE TABLE `walker_transaction` (
          `id` bigint(20) DEFAULT NULL COMMENT '唯一标示',
          `app_id` varchar(64) DEFAULT NULL COMMENT '名称',
          `master_gid` varchar(64) DEFAULT NULL COMMENT '名称',
          `branch_gid` varchar(64) DEFAULT NULL COMMENT '名称',
          `is_declare` bit(1) DEFAULT '0' COMMENT '1 声明者 2 跟随者',
          `status` char(1) DEFAULT '1' COMMENT '状态 1启用 0 停用',
          `gmt_create` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
          `gmt_modified` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
          `is_deleted` char(1) DEFAULT '0' COMMENT '0 未删除 1 逻辑删除'
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
     */


}

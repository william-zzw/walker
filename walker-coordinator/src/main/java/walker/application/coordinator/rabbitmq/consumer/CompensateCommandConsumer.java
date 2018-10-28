package walker.application.coordinator.rabbitmq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import walker.application.coordinator.service.WalkerTransactionService;
import walker.protocol.message.RabbitConst;
import walker.protocol.message.WalkerMessage;
import walker.protocol.message.command.CompensateCommand;
import walker.rabbitmq.WalkerMessageUtils;

import javax.annotation.Resource;

@Service
@Slf4j
public class CompensateCommandConsumer {

    @Resource
    private WalkerTransactionService walkerTransactionService;

    @RabbitListener(
            admin = RabbitConst.RABBITMQ_ADMIN_NAME,
            bindings = @QueueBinding(
                    //1.test.demo.send:队列名,2.true:是否长期有效,3.false:是否自动删除
                    value = @Queue(value = RabbitConst.WALKER_COMPENSATE_QUEUE, durable = "true", autoDelete = "false", exclusive = "false"),
                    //1.default.topic交换器名称(默认值),2.true:是否长期有效,3.topic:类型是topic
                    exchange = @Exchange(value = RabbitConst.EXCHANGE_NAME, durable = "true", type = RabbitConst.EXCHANGE_TYPE_TOPIC),
                    //test2.send:路由的名称,ProducerConfig 里面 绑定的路由名称(xxxx.to(exchange).with("test2.send")))
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
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); // false只确认当前一个消息收到，true确认所有consumer获得的消息
        }
    }

    @Async
    public void handleCommit(CompensateCommand.CompensateCommit toCommand) {
        /*
        app_id
        master_gid
        branch_gid
        notify_url
        notify_body

         */

    }

    @Async
    public void handleBroken(CompensateCommand.CompensateBroken toCommand) {

    }

}

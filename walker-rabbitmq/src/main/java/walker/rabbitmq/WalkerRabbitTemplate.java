package walker.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import walker.protocol.message.RabbitConst;
import walker.protocol.message.WalkerMessage;

import java.util.UUID;

/**
 * example
 WalkerMessage walkerMessage = WalkerMessageUtils.toWalkerMessage(RouteToDemoCommand.SIMPLE, command);
 rabbitSender.send(RabbitConst.ROUTE_TO_DEMO, walkerMessage);
 */
@Component("walkerRabbitTemplate")
public class WalkerRabbitTemplate implements RabbitTemplate.ConfirmCallback{

    @Autowired
    @Qualifier("rabbitTemplate")
    private RabbitTemplate rabbitTemplate;

    public void send(String routingKey, WalkerMessage walkerMessage) {
        this.doSend(RabbitConst.EXCHANGE_NAME, routingKey, walkerMessage);
    }


    void doSend(String exchange, String routingKey, WalkerMessage message) {
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());
        rabbitTemplate.convertAndSend(exchange, routingKey, WalkerMessageUtils.toRabbitTccMessage(message), correlationData);
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean b, String s) {

    }
}

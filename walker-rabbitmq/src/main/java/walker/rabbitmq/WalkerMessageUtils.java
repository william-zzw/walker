package walker.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import walker.common.WalkerConst;
import walker.common.context.WalkerContext;
import walker.common.context.WalkerContextlManager;
import walker.common.util.GsonUtils;
import walker.protocol.message.WalkerMessage;

@Slf4j
public class WalkerMessageUtils {

    public static <T> WalkerMessage toWalkerMessage(int command, T t) {
        String payload = walker.protocol.util.GsonUtils.getGson().toJson(t);
        return new WalkerMessage(command, payload);
    }

    public static <T> T toCommand(WalkerMessage message, Class<T> t) {
        return walker.protocol.util.GsonUtils.fromJson(message.getPayload(), t);
    }

    public static Message toRabbitTccMessage(WalkerMessage msg) {
        Message message = null;
        try {
            message = MessageBuilder.withBody(GsonUtils.getGson().toJson(msg).getBytes("UTF-8")).build();
            if (message != null) {
                MessageProperties messageProperties = message.getMessageProperties();
                WalkerContext walkerContext = WalkerContextlManager.initIfContextNull();
                if (walkerContext != null) {
                    messageProperties.setHeader(WalkerConst.WALKER_MASTER_GID, walkerContext.getMasterGid());
                }
            }
        } catch (Exception e) {
            log.error("toRabbitTccMessage error", e);
        }
        return message;
    }

    public static WalkerMessage fromAmqpMessage(Message message) {
        WalkerMessage msg = null;
        try {
            msg = GsonUtils.getGson().fromJson(new String(message.getBody(), "UTF-8"), WalkerMessage.class);
            msg.setAttachment(message.getMessageProperties().getHeaders());
            if (message.getMessageProperties().getHeaders().size() > 0) {
                Object masterGid = message.getMessageProperties().getHeaders().get(WalkerConst.WALKER_MASTER_GID);
                if (masterGid != null) {
                    WalkerContextlManager.inherit(String.valueOf(masterGid));
                }
            }
        } catch (Exception e) {
            log.error("from RabbitMQ Message convert to WalkerMessage error,", e);
        }
        return msg;
    }

}

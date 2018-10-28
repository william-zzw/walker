package walker.protocol.message;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Data
public class WalkerMessage implements TimeoutMessage {

    private String taskId;
    private String payload;
    private int command;

    private Map<String, Object> attachment;

    public WalkerMessage() {
        taskId = UUID.randomUUID().toString();
    }

    public WalkerMessage(String payload) {
        this.taskId = UUID.randomUUID().toString();
        this.payload = payload;
        this.command = -1;
    }

    public WalkerMessage(int command, String payload) {
        this.taskId = UUID.randomUUID().toString();
        this.payload = payload;
        this.command = command;
    }

//    public Message toRabbitMessage() {
//        Gson gson = new Gson();
//        Message message = null;
//        try {
//            message = MessageBuilder.withBody(gson.toJson(this).getBytes("UTF-8")).build();
//        } catch (Exception e) {
//            log.error("to rabbit Message error,", e);
//        }
//        return message;
//    }
}

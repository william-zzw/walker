package walker.application.coordinator.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import walker.rabbitmq.control.MessageListenerContainerController;

@RestController
@RequestMapping("/rabbit/manage")
public class RabbitManageController {

    @Autowired(required = false)
    private MessageListenerContainerController messageListenerContainerController;

    /**
     * 重置指定队列消费者数量
     *
     * @param queueName
     * @param concurrentConsumers
     * @return
     */
    @GetMapping("resetConcurrentConsumers")
    public boolean resetConcurrentConsumers(String queueName, int concurrentConsumers) {
        return messageListenerContainerController.resetQueueConcurrentConsumers(queueName, concurrentConsumers);
    }

    /**
     * 重启对消息队列的监听
     *
     * @param queueName
     * @return
     */
    @GetMapping("restartMessageListener")
    public boolean restartMessageListener(String queueName) {
        return messageListenerContainerController.restartMessageListener(queueName);
    }

    /**
     * 停止对消息队列的监听
     *
     * @param queueName
     * @return
     */
    @GetMapping("stopMessageListener")
    public boolean stopMessageListener(String queueName) {
        return messageListenerContainerController.stopMessageListener(queueName);
    }

    /**
     * 获取所有消息队列对应的消费者
     *
     * @return
     */
    @GetMapping("statAllMessageQueueDetail")
    public List<MessageListenerContainerController.MessageQueueInfo> statAllMessageQueueDetail() {
        return messageListenerContainerController.statAllMessageQueueDetail();
    }

}

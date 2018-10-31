package walker.rabbitmq.control;


import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
消费者监听队列需要与MQServer保持长连接，平时数据压力不大时，多个消费者同时监听一个队列，对系统资源也是一种浪费。动态修改消费者数量可以简单可靠的解决MQServer数据堆积问题。
@see org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer#start
从源码可知，volatile变量concurrentConsumers，可以保证所有线程对它的可见性，因此可以通过修改该字段来改变某个队列的消费者数量，达到动态改变消息消费速度的目的，以应对消息堆积的场景。
 */
@Component
public class MessageListenerContainerController {

    private static final String CONTAINER_NOT_EXISTS = "消息队列%s对应的监听容器不存在！";

    @Autowired
    private RabbitListenerEndpointRegistry registry;

    /**
     * queue2ContainerAllMap初始化标识
     */
    private volatile boolean hasInit = false;

    /**
     * 所有的队列监听容器MAP
     */
    private final Map<String, SimpleMessageListenerContainer> allQueue2ContainerMap = new ConcurrentHashMap<String, SimpleMessageListenerContainer>();

    /**
     * 重置消息队列并发消费者数量
     *
     * @param queueName
     * @param concurrentConsumers must greater than zero
     * @return
     */
    public boolean resetQueueConcurrentConsumers(String queueName, int concurrentConsumers) {
        Assert.state(concurrentConsumers > 0, "参数 'concurrentConsumers' 必须大于0.");
        SimpleMessageListenerContainer container = findContainerByQueueName(queueName);
        if (container.isActive() && container.isRunning()) {
            container.setConcurrentConsumers(concurrentConsumers);
            return true;
        }
        return false;
    }


    /**
     * 重启对消息队列的监听
     *
     * @param queueName
     * @return
     */
    public boolean restartMessageListener(String queueName) {
        SimpleMessageListenerContainer container = findContainerByQueueName(queueName);
        Assert.state(!container.isRunning(), String.format("消息队列%s对应的监听容器正在运行！", queueName));
        container.start();
        return true;
    }

    /**
     * 停止对消息队列的监听
     *
     * @param queueName
     * @return
     */
    public boolean stopMessageListener(String queueName) {
        SimpleMessageListenerContainer container = findContainerByQueueName(queueName);
        Assert.state(container.isRunning(), String.format("消息队列%s对应的监听容器未运行！", queueName));
        container.stop();
        return true;
    }

    /**
     * 统计所有消息队列详情
     *
     * @return
     */
    public List<MessageQueueInfo> statAllMessageQueueDetail() {
        List<MessageQueueInfo> queueDetailList = new ArrayList<>();
        getQueue2ContainerAllMap().entrySet().forEach(entry -> {
            String queueName = entry.getKey();
            SimpleMessageListenerContainer container = entry.getValue();
            MessageQueueInfo deatil = new MessageQueueInfo(queueName, container);
            queueDetailList.add(deatil);
        });

        return queueDetailList;
    }

    /**
     * 根据队列名查找消息监听容器
     *
     * @param queueName
     * @return
     */
    private SimpleMessageListenerContainer findContainerByQueueName(String queueName) {
        String key = StringUtils.trim(queueName);
        SimpleMessageListenerContainer container = getQueue2ContainerAllMap().get(key);
        Assert.notNull(container, String.format(CONTAINER_NOT_EXISTS, key));
        return container;
    }

    private Map<String, SimpleMessageListenerContainer> getQueue2ContainerAllMap() {
        if (!hasInit) {
            synchronized (allQueue2ContainerMap) {
                if (!hasInit) {
                    registry.getListenerContainers().forEach(container -> {
                        SimpleMessageListenerContainer simpleContainer = (SimpleMessageListenerContainer) container;
                        Arrays.stream(simpleContainer.getQueueNames()).forEach(queueName ->
                                allQueue2ContainerMap.putIfAbsent(StringUtils.trim(queueName), simpleContainer));
                    });
                    hasInit = true;
                }
            }
        }
        return allQueue2ContainerMap;
    }


    /**
     * 消息队列详情
     *
     * @author liuzhe
     * @date 2018/04/04
     */
    @Data
    public static final class MessageQueueInfo {
        /**
         * 队列名称
         */
        private String queueName;

        /**
         * 监听容器标识
         */
        private String containerIdentity;

        /**
         * 监听是否有效
         */
        private boolean activeContainer;

        /**
         * 是否正在监听
         */
        private boolean running;

        /**
         * 活动消费者数量
         */
        private int activeConsumerCount;

        public MessageQueueInfo(String queueName, SimpleMessageListenerContainer container) {
            this.queueName = queueName;
            this.running = container.isRunning();
            this.activeContainer = container.isActive();
            this.activeConsumerCount = container.getActiveConsumerCount();
            this.containerIdentity = "Container@" + ObjectUtils.getIdentityHexString(container);
        }

    }

}
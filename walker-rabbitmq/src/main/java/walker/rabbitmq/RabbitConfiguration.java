package walker.rabbitmq;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.*;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import walker.protocol.message.RabbitConst;
import walker.rabbitmq.config.RabbitMQConfig;

import java.util.concurrent.*;

@Configuration
@ConditionalOnClass(value = {org.springframework.amqp.rabbit.core.RabbitTemplate.class})
@ComponentScan(basePackages = {"walker.rabbitmq"})
public class RabbitConfiguration {

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Bean(name = "rabbitConnectFactory")
    @Primary
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(rabbitMQConfig.getAddresses() + ":" + rabbitMQConfig.getPort());
        connectionFactory.setUsername(rabbitMQConfig.getUsername());
        connectionFactory.setPassword(rabbitMQConfig.getPassword());
        connectionFactory.setVirtualHost(rabbitMQConfig.getVirtualHost());

        // 关键所在，指定线程池
        // 接通RabbitMQ 后,系统开辟20个通道 处理这些信息
        ExecutorService service = Executors.newFixedThreadPool(20);
        connectionFactory.setExecutor(service);
        connectionFactory.setConnectionTimeout(15000);// 15秒
        /** 如果要进行消息回调，则这里必须要设置为true */
        connectionFactory.setPublisherConfirms(rabbitMQConfig.isPublisherConfirms());
        return connectionFactory;
    }

    @Bean(name = RabbitConst.RABBITMQ_ADMIN_NAME)
    public RabbitAdmin rabbitAdmin(@Qualifier("rabbitConnectFactory") ConnectionFactory connectionFactory) {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        return rabbitAdmin;
    }

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(RabbitConst.EXCHANGE_NAME);
    }

    /**
     * Queue
     *
     * @return
     */

    @Bean(name = RabbitConst.WALKER_TRANSACTION_QUEUE)
    public Queue walkerTransactionQueue(RabbitAdmin rabbitAdmin) {
        //1.队列名称,2.声明一个持久队列,3.声明一个独立队列,4.如果服务器在不再使用时自动删除队列
        Queue queue = new Queue(RabbitConst.WALKER_TRANSACTION_QUEUE, true, false, false);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    @Bean(name = RabbitConst.WALKER_COMPENSATE_QUEUE)
    public Queue walkerCompensateQueue(RabbitAdmin rabbitAdmin) {
        //1.队列名称,2.声明一个持久队列,3.声明一个独立队列,4.如果服务器在不再使用时自动删除队列
        Queue queue = new Queue(RabbitConst.WALKER_COMPENSATE_QUEUE, true, false, false);
        rabbitAdmin.declareQueue(queue);
        return queue;
    }

    @Bean(name = RabbitConst.ROUTE_TO_REPORT_TRANSACTION)
    Binding binding_report_transaction_queue(@Qualifier(RabbitConst.WALKER_TRANSACTION_QUEUE) Queue walkerTransactionQueue, TopicExchange exchange) {
        return BindingBuilder.bind(walkerTransactionQueue).to(exchange).with(RabbitConst.ROUTE_TO_REPORT_TRANSACTION);
    }

    @Bean(name = RabbitConst.ROUTE_TO_REPORT_COMPENSATE)
    Binding binding_report_compensate_queue(@Qualifier(RabbitConst.WALKER_COMPENSATE_QUEUE) Queue walkerCompensateQueue, TopicExchange exchange) {
        return BindingBuilder.bind(walkerCompensateQueue).to(exchange).with(RabbitConst.ROUTE_TO_REPORT_COMPENSATE);
    }
    /*
    @Bean(name = RabbitConst.ROUTE_FOR_MASTER_TX_COMMIT)
    Binding binding_master_transaction_commit(@Qualifier(RabbitConst.WALKER_TRANSACTION_QUEUE) Queue masterTransactionQueue, TopicExchange exchange) {
        return BindingBuilder.bind(masterTransactionQueue).to(exchange).with(RabbitConst.ROUTE_FOR_MASTER_TX_COMMIT);
    }

    @Bean(name = RabbitConst.ROUTE_FOR_BRANCH_TX_REG)
    Binding binding_branch_transaction_reg(@Qualifier(RabbitConst.NAME_OF_BRANCH_TCC_TRANSACTION_QUEUE) Queue branchTransactionQueue, TopicExchange exchange) {
        return BindingBuilder.bind(branchTransactionQueue).to(exchange).with(RabbitConst.ROUTE_FOR_BRANCH_TX_REG);
    }

    @Bean(name = RabbitConst.ROUTE_FOR_BRANCH_TX_COMMIT)
    Binding binding_branch_transaction_commit(@Qualifier(RabbitConst.NAME_OF_BRANCH_TCC_TRANSACTION_QUEUE) Queue branchTransactionQueue, TopicExchange exchange) {
        return BindingBuilder.bind(branchTransactionQueue).to(exchange).with(RabbitConst.ROUTE_FOR_BRANCH_TX_COMMIT);
    }*/

    @Bean(name = "retryTemplate")
    @Primary
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new org.springframework.retry.support.RetryTemplate();
        ExponentialBackOffPolicy exponentialBackOffPolicy = new org.springframework.retry.backoff.ExponentialBackOffPolicy();
        exponentialBackOffPolicy.setInitialInterval(500);
        exponentialBackOffPolicy.setMaxInterval(10000);
        exponentialBackOffPolicy.setMultiplier(10.0);
        retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
        return retryTemplate;
    }

    /**
     * <rabbit:template id="rabbitTemplate" connection-factory="rabbitConnectionFactory"
     * retry-template="retryTemplate" reply-timeout="60000"/>
     */
    @Bean(name = "rabbitTemplate")
    /** 因为要设置回调类，所以应是prototype类型，如果是singleton类型，则回调类为最后一次设置 */
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @Primary
    public RabbitTemplate rabbitTemplate(@Qualifier("rabbitConnectFactory") ConnectionFactory connectionFactory, @Qualifier("retryTemplate") RetryTemplate retryTemplate) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setRetryTemplate(retryTemplate);
        return rabbitTemplate;
    }

    @Bean(name = "simpleRabbitListenerContainerFactory")
    @Primary
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory(@Qualifier("rabbitConnectFactory") ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(new Jackson2JsonMessageConverter());
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    @Bean(name = "simpleMessageListenerContainer")
    @Primary
    public SimpleMessageListenerContainer simpleMessageListenerContainer(@Qualifier("rabbitConnectFactory") ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        int keepAliveSeconds = 30;
        int queueCapacity = 100; // 可以提交到任务处理队列的数量

        BlockingQueue<Runnable> queue = new LinkedBlockingQueue(queueCapacity);
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("rabbit-consumer-pool-%d").build();
        RejectedExecutionHandler rejectedExecutionHandler = new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy();

        int MAX_CURRENT_CONSUME_MSG_TPS = 10;
        container.setConcurrentConsumers(MAX_CURRENT_CONSUME_MSG_TPS); // 控制消费速度, 等同于同时多少个consumer工作
        container.setPrefetchCount(MAX_CURRENT_CONSUME_MSG_TPS);

        // 最好是1个消费者1个线程 ,当然也适量控制线程总量
        int MAX_CONSUMER_THREAD_COUNT = 100;
        int MIN_CONSUMER_THREAD_COUNT = MAX_CURRENT_CONSUME_MSG_TPS;
        ThreadPoolExecutor consumerPoolExecutor = new ThreadPoolExecutor(MIN_CONSUMER_THREAD_COUNT, MAX_CONSUMER_THREAD_COUNT, (long) keepAliveSeconds, TimeUnit.SECONDS, queue, namedThreadFactory, rejectedExecutionHandler);

        // taskExecutor作用是初始化consumer监听来着RABBITMQ的消息, 数量越多消费者实例就越多, 根据真实的consumer适当调整
        container.setTaskExecutor(consumerPoolExecutor);
        return container;
    }

}

package walker.application;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import walker.rabbitmq.RabbitConfiguration;

@Configuration
@ComponentScan(basePackages = {"walker.application.infrastructure"})
@Import(RabbitConfiguration.class)
public class AppConfiguration {

}
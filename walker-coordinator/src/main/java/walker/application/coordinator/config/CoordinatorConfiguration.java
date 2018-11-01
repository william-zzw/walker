package walker.application.coordinator.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import walker.rabbitmq.RabbitConfiguration;

/**
 * @author SONG
 */
@Configuration
@ComponentScan(basePackages = {"walker.application.infrastructure"})
@Import(RabbitConfiguration.class)
public class CoordinatorConfiguration {

}
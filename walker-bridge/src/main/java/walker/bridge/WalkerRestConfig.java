package walker.bridge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import walker.bridge.interceptor.WalkerFeignInterceptor;
import walker.bridge.interceptor.WalkerRestInterceptor;

@Configuration
@ConditionalOnClass({javax.servlet.http.HttpServletRequest.class, feign.RequestTemplate.class})
@ComponentScan(value = {"walker.bridge.interceptor"})
public class WalkerRestConfig extends WebMvcConfigurationSupport {

    @Autowired
    private WalkerRestInterceptor walkerRestInterceptor;

    @Bean
    public WalkerFeignInterceptor walkerFeignInterceptor() {
        return new WalkerFeignInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(walkerRestInterceptor);
    }
}

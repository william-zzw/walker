package walker.core.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="walker.proxy")
@Setter
@Getter
public class WalkerProxyProperties {

    private String appId;

}

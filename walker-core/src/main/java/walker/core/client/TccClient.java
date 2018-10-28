package walker.core.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class TccClient {

    @Value("${walker.client.name:walker.core.client.TccClient.walkerClientName}")
    private String walkerClientName;

}
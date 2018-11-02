package walker.application;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource(locations = {"classpath:/spring/notify-spring.xml"})
public class NotifyApplication {

    public static void main(String[] args) {

    }

}

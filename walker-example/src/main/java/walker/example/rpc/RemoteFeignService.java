package walker.example.rpc;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import walker.example.model.YourBusinessModel;

@Component("remoteFeignService")
@FeignClient(name = "SPRING-PRODUCER-SERVER/remote")
public interface RemoteFeignService {

    @RequestMapping(value = "/pay", method = RequestMethod.POST)
    ResponseEntity<String> pay(@RequestBody YourBusinessModel model);

}

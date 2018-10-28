package walker.core.method.compensate;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MethodTccCompensateEndpointRegistry {

    private volatile Map<String, TccCompensateEndpointContainer> tccCompensateContainers = new ConcurrentHashMap();

    public void registerTccCompensateEndpoint(MethodTccCompensateEndpoint endpoint, String container) {
        Assert.hasText(container, "Endpoint container must not be empty");
        Assert.notNull(endpoint, "Endpoint must not be null");
        String identifier = StringUtils.isEmpty(endpoint.getId()) ? endpoint.getTryMethodName() : endpoint.getId();
//        Assert.hasText(id, "Endpoint id must not be empty");
        synchronized (this.tccCompensateContainers) {
            boolean containsContainer = this.tccCompensateContainers.containsKey(container);
            TccCompensateEndpointContainer tccCompensateEndpointContainer = null;
            if (containsContainer) {
                tccCompensateEndpointContainer = this.tccCompensateContainers.get(container);
            } else {
                tccCompensateEndpointContainer = new TccCompensateEndpointContainer();
                this.tccCompensateContainers.putIfAbsent(container, tccCompensateEndpointContainer);
            }
            Assert.state(!tccCompensateEndpointContainer.containsKey(identifier), "Another endpoint is already registered with id '" + identifier + "'");
            tccCompensateEndpointContainer.put(identifier, endpoint);
        }
    }

    public TccCompensateEndpointContainer getTccCompensateContainer(String id) {
        Assert.hasText(id, "Container identifier must not be empty");
        Assert.state(!tccCompensateContainers.containsKey(id), "the endpoint container is not exists '" + id + "'");
        return this.tccCompensateContainers.get(id);
    }

    public Set<String> getTccCompensateContainerIds() {
        return Collections.unmodifiableSet(this.tccCompensateContainers.keySet());
    }

    public Collection<TccCompensateEndpointContainer> getTccCompensateContainers() {
        return Collections.unmodifiableCollection(this.tccCompensateContainers.values());
    }

    public MethodTccCompensateEndpoint getMethodTccCompensateEndpoint(String container, String identifier) {
        Assert.state(!tccCompensateContainers.containsKey(container), "the endpoint container is not exists '" + container + "'");
        TccCompensateEndpointContainer endpointContainer = tccCompensateContainers.get(container);
        Assert.state(!endpointContainer.containsKey(identifier), "the endpoint container " + container + "is not exists '" + identifier + "'");
        return endpointContainer.get(identifier);
    }

    public static class TccCompensateEndpointContainer extends ConcurrentHashMap<String, MethodTccCompensateEndpoint> {

    }

}

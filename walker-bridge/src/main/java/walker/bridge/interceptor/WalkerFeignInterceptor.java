package walker.bridge.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import walker.common.WalkerConst;
import walker.common.context.WalkerContext;
import walker.common.context.WalkerContextlManager;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

public class WalkerFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                String values = request.getHeader(name);
                requestTemplate.header(name, values);
            }
        }
        WalkerContext walkerContext = WalkerContextlManager.initIfContextNull();
        if (walkerContext != null) {
            requestTemplate.header(WalkerConst.WALKER_MASTER_GID, walkerContext.getMasterGid());
        }
    }
}

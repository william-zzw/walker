package walker.bridge.interceptor;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import walker.common.WalkerConst;
import walker.common.context.WalkerContextlManager;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Order(value = Ordered.LOWEST_PRECEDENCE - 1)
public class WalkerRestInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws Exception {
        String walkerMasterGid = httpServletRequest.getHeader(WalkerConst.WALKER_MASTER_GID);
        if (walkerMasterGid != null) {
            WalkerContextlManager.inherit(walkerMasterGid);
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler, ModelAndView modelAndView) throws Exception {

    }

    /**
     * 控制器方法抛不抛异常都会被调用
     *
     * @param httpServletRequest
     * @param httpServletResponse
     * @param o
     * @param e
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        WalkerContextlManager.clear();
    }
}

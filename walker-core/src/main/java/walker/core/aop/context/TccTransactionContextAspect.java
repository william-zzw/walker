package walker.core.aop.context;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import walker.common.annotation.TccTransaction;
import walker.common.context.WalkerContext;
import walker.common.context.WalkerContextlManager;
import walker.core.client.WalkerProxy;

@Aspect
@Component
@Order(value = Ordered.LOWEST_PRECEDENCE + 1)
@Slf4j
public class TccTransactionContextAspect {

    @Autowired
    private WalkerProxy walkerProxy;

    @Pointcut(value = "@annotation(walker.common.annotation.TccTransaction)")
    private void tccTransactionContextPointcut() {
    }

    @Before("@annotation(tcc)")
    public void doBefore(TccTransaction tcc, JoinPoint point) {
        log.info("HI ,point:{}", endpoint(point));
        WalkerContextlManager.incrementDepth();

        boolean declare = false;
        WalkerContext walkerContext = WalkerContextlManager.initIfContextNull();
        if (StringUtils.isEmpty(walkerContext.getMasterGid())) {
            WalkerContextlManager.startMaster();
            declare = true;
        }else {
            WalkerContextlManager.startBranch();
        }
        walkerProxy.transactionStart(WalkerContextlManager.getContext(), declare);
    }

    private String endpoint(JoinPoint point) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(point.getTarget().getClass().getName());
        stringBuffer.append("##");
        stringBuffer.append(((MethodSignature) point.getSignature()).getMethod().getName());
        return stringBuffer.toString();
    }

    @After("tccTransactionContextPointcut()")
    public void doAfter(JoinPoint point) {
        log.info("BYE ,point:{}", endpoint(point));
        if (WalkerContextlManager.isFirstDepth()) {
//            profile("commit");
            WalkerContextlManager.clear();
        } else {
            WalkerContextlManager.decrementDepth();
        }
    }

    @AfterThrowing(value = "tccTransactionContextPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint point,Throwable e) throws Throwable {
        log.info("ERROR ,point:{}", endpoint(point));
        log.error("process abort", e);

        WalkerContext walkerContext = WalkerContextlManager.getContext();
        if (walkerContext.getException() == null) {
            walkerContext.setException(e);
        }
        if (WalkerContextlManager.isFirstDepth()) {
//            profile("rollback");
            WalkerContextlManager.clear();
        } else {
            WalkerContextlManager.decrementDepth();
        }
        throw e;
    }

}
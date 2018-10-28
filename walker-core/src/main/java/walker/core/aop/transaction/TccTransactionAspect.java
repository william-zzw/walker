package walker.core.aop.transaction;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import walker.common.annotation.TccTransaction;
import walker.common.compensate.CompensateMode;
import walker.common.context.WalkerContext;
import walker.common.context.WalkerContextlManager;
import walker.core.client.WalkerProxy;
import walker.core.method.compensate.MethodTccCompensateEndpoint;
import walker.core.method.compensate.MethodTccCompensateEndpointRegistry;
import walker.core.tools.CompensateUtils;
import walker.core.warpper.DefaultTccTransactionResultJudge;
import walker.core.warpper.TccTransactionResultJudge;
import walker.protocol.compensate.Participant;

import java.lang.reflect.Method;

/**
 * 执行顺序: doAround -1 doBefore doAround -2 doAfter doAfterReturning
 *
 * @date 2018/10/14
 */
@Aspect
@Component
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class TccTransactionAspect implements InitializingBean {

    public static final String LOG_KEY = "TccTransaction#";

    @Autowired(required = false)
    private TccTransactionResultJudge tccTransactionResultJudge;

    @Autowired
    private WalkerProxy walkerProxy;

    @Autowired
    private MethodTccCompensateEndpointRegistry methodTccCompensateEndpointRegistry;

    @Pointcut(value = "@annotation(walker.common.annotation.TccTransaction)")
    private void tccTransactionPointcut() { }

    @Around(value = "@annotation(tcc)")
    public Object doAround(TccTransaction tcc, ProceedingJoinPoint pjp) throws Throwable {
        String serviceName = pjp.getTarget().getClass().getName();
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        Object result = null;
        try {
            result = pjp.proceed();
            if (!method.getReturnType().equals(Void.class)) {
                if (tcc.needJudge()) {
                    if (!tccTransactionResultJudge.ok(result)) {
                        throw new RuntimeException("bad result");
                    }
                }
            }
            if (tcc.mode() == CompensateMode.SYNC) {
                prepareCompensate(WalkerContextlManager.getContext(), tcc, serviceName, pjp);
            } else {
                asyncPrepareCompensate(WalkerContextlManager.getContext(), tcc, serviceName, pjp);
            }
            walkerProxy.transactionCommit(WalkerContextlManager.getContext());
            return result;
        } catch (Throwable ex) {
            walkerProxy.transactionBroken(WalkerContextlManager.getContext(), ex);
            throw ex;
        }
    }
    private void prepareCompensate(WalkerContext tccContext, TccTransaction tcc, String serviceName, ProceedingJoinPoint pjp) {
        try {
            MethodTccCompensateEndpointRegistry.TccCompensateEndpointContainer tccCompensateEndpointContainer = methodTccCompensateEndpointRegistry.getTccCompensateContainer(serviceName);
            MethodTccCompensateEndpoint methodTccCompensateEndpoint = tccCompensateEndpointContainer.get(tcc.id());

            Participant commitParticipant = (Participant) methodTccCompensateEndpoint.getCommitMethod().invoke(methodTccCompensateEndpoint.getBean(), CompensateUtils.getValue(pjp, methodTccCompensateEndpoint.getCommitMethodArgs()));
            Participant cancelParticipant = (Participant) methodTccCompensateEndpoint.getCancelMethod().invoke(methodTccCompensateEndpoint.getBean(), CompensateUtils.getValue(pjp, methodTccCompensateEndpoint.getCancelMethodArgs()));
            walkerProxy.compensateCommit(tccContext, commitParticipant, cancelParticipant);
        } catch (Exception e) {
            log.error("asyncPrepareCompensate Exception", e);
            walkerProxy.compensateBroken(tccContext, e);
        }
    }

    @Async
    public void asyncPrepareCompensate(WalkerContext tccContext, TccTransaction tcc, String serviceName, ProceedingJoinPoint pjp) {
        prepareCompensate(tccContext, tcc, serviceName, pjp);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (tccTransactionResultJudge == null) {
            tccTransactionResultJudge = new DefaultTccTransactionResultJudge();
        }
    }
}

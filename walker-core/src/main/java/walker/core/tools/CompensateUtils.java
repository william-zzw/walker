package walker.core.tools;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;

public class CompensateUtils {

    public static Object[] getValue(JoinPoint joinPoint, String[] condition) {
        return getValue(joinPoint.getTarget(), joinPoint.getArgs(), joinPoint.getTarget().getClass(), ((MethodSignature) joinPoint.getSignature()).getMethod(), condition);
    }

    private static Object[] getValue(Object object, Object[] args, Class<?> clazz, Method method, String[] condition) {
        if (args == null) {
            return null;
        }
        AnnotationExpressionEvaluator<Object> evaluator = new AnnotationExpressionEvaluator<>();
        EvaluationContext evaluationContext = evaluator.createEvaluationContext(object, clazz, method, args);
        AnnotatedElementKey methodKey = new AnnotatedElementKey(method, clazz);
        Object[] values = new Object[condition.length];
        for (int i = 0; i < condition.length; i++) {
            values[i] = evaluator.condition(condition[i], methodKey, evaluationContext, Object.class);
        }
        return values;
    }

    public static Class<?> findTargetClass(Object proxy) throws Exception {
        if (AopUtils.isAopProxy(proxy)) {
            AdvisedSupport advised = getAdvisedSupport(proxy);
            Object target = advised.getTargetSource().getTarget();
            return findTargetClass(target);
        } else {
            return proxy.getClass();
        }
    }

    private static AdvisedSupport getAdvisedSupport(Object proxy) throws Exception {
        Field h;
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            h = proxy.getClass().getSuperclass().getDeclaredField("h");
        } else {
            h = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
        }
        h.setAccessible(true);
        Object dynamicAdvisedInterceptor = h.get(proxy);
        Field advised = dynamicAdvisedInterceptor.getClass().getDeclaredField("advised");
        advised.setAccessible(true);
        return (AdvisedSupport) advised.get(dynamicAdvisedInterceptor);
    }

}

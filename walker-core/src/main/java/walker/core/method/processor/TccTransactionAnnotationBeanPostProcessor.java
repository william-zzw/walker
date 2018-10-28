package walker.core.method.processor;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import walker.common.annotation.TccTransaction;
import walker.core.method.compensate.MethodTccCompensateEndpoint;
import walker.core.method.compensate.MethodTccCompensateEndpointRegistry;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;


public class TccTransactionAnnotationBeanPostProcessor implements BeanPostProcessor, Ordered {


    private final AtomicInteger counter = new AtomicInteger();
    private final ConcurrentMap<Class<?>, TypeMetadata> typeCache = new ConcurrentHashMap();

    @Autowired
    private MethodTccCompensateEndpointRegistry registry;


    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        TccTransactionAnnotationBeanPostProcessor.TypeMetadata metadata = this.typeCache.computeIfAbsent(targetClass, this::buildMetadata);
        TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod[] tccTransactionMethods = metadata.transactionMethods;
        int tccTransactionMethodCount = tccTransactionMethods.length;
        for (int i = 0; i < tccTransactionMethodCount; ++i) {
            TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod lm = tccTransactionMethods[i];
            TccTransaction tccTransaction = lm.annotation;
            this.processTccTransaction(tccTransaction, lm.method, bean, beanName);
        }
        return bean;
    }

    private void processTccTransaction(TccTransaction tccTransaction, Method method, Object bean, String beanName) {
        Method methodToUse = this.checkProxy(method, bean);
        MethodTccCompensateEndpoint endpoint = new MethodTccCompensateEndpoint();
        endpoint.setTryMethod(methodToUse);
        endpoint.setTryParameterTypes(methodToUse.getParameterTypes());
        this.processTransaction(endpoint, tccTransaction, bean, beanName);
    }

    private void processTransaction(MethodTccCompensateEndpoint endpoint, TccTransaction tccTransaction, Object bean, String beanName) {
        endpoint.setBean(bean);
        endpoint.setId(this.getEndpointId(tccTransaction));
        String commitMethodName = tccTransaction.commitMethodName();
        Method commitMethod = ReflectionUtils.findMethod(bean.getClass(), commitMethodName);
        endpoint.setCommitMethod(commitMethod);
        endpoint.setCommitMethodName(commitMethodName);
        endpoint.setCommitParameterTypes(commitMethod.getParameterTypes());
        endpoint.setCommitMethodArgs(tccTransaction.commitMethodArgs());

        String cancelMethodName = tccTransaction.cancelMethodName();
        Method cancelMethod = ReflectionUtils.findMethod(bean.getClass(), cancelMethodName);
        endpoint.setCancelMethod(cancelMethod);
        endpoint.setCancelMethodName(cancelMethodName);
        endpoint.setCancelParameterTypes(cancelMethod.getParameterTypes());
        endpoint.setCancelMethodArgs(tccTransaction.cancelMethodArgs());

        this.registry.registerTccCompensateEndpoint(endpoint, beanName);
    }

    private String getEndpointId(TccTransaction tccTransaction) {
        return StringUtils.hasText(tccTransaction.id()) ? this.resolveId(tccTransaction.id()) : this.resolveId(this.counter.getAndIncrement());
    }

    private String resolveId(String id) {
        return "walker_endpoint_" + id;
    }

    private String resolveId(Integer id) {
        return "walker_endpoint_" + id;
    }

    private TccTransactionAnnotationBeanPostProcessor.TypeMetadata buildMetadata(Class<?> targetClass) {
        List<TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod> methods = new ArrayList();
        ReflectionUtils.doWithMethods(targetClass, (method) -> {
            Collection<TccTransaction> listenerAnnotations = this.findTccTransactionAnnotations(method);
            if (listenerAnnotations.size() > 0) {
                methods.add(new TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod(method, listenerAnnotations.toArray(new TccTransaction[listenerAnnotations.size()])));
            }
        }, ReflectionUtils.USER_DECLARED_METHODS);
        return methods.isEmpty()
                ?
                TccTransactionAnnotationBeanPostProcessor.TypeMetadata.EMPTY :
                new TccTransactionAnnotationBeanPostProcessor.TypeMetadata(methods.toArray(new TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod[methods.size()]));
    }

    private Collection<TccTransaction> findTccTransactionAnnotations(Method method) {
        Set<TccTransaction> listeners = new HashSet();
        TccTransaction ann = (TccTransaction) AnnotationUtils.findAnnotation(method, TccTransaction.class);
        if (ann != null) {
            listeners.add(ann);
        }
        return listeners;
    }

    private static class TypeMetadata {
        final TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod[] transactionMethods;

        static final TccTransactionAnnotationBeanPostProcessor.TypeMetadata EMPTY = new TccTransactionAnnotationBeanPostProcessor.TypeMetadata();

        public TypeMetadata() {
            this.transactionMethods = new TccTransactionAnnotationBeanPostProcessor.TccTransactionMethod[0];
        }

        TypeMetadata(TccTransactionMethod[] listenerMethods) {
            this.transactionMethods = listenerMethods;
        }
    }

    private static class TccTransactionMethod {
        final Method method;
        final TccTransaction annotation;

        private TccTransactionMethod(Method method, TccTransaction[] tccTransaction) {
            this.method = method;
            this.annotation = tccTransaction[0];
        }
    }

    private Method checkProxy(Method method, Object bean) {
        if (AopUtils.isJdkDynamicProxy(bean)) {
            try {
                method = bean.getClass().getMethod(method.getName(), method.getParameterTypes());
                Class<?>[] proxiedInterfaces = ((Advised) bean).getProxiedInterfaces();
                Class[] var4 = proxiedInterfaces;
                int var5 = proxiedInterfaces.length;
                int var6 = 0;
                while (var6 < var5) {
                    Class iface = var4[var6];
                    try {
                        method = iface.getMethod(method.getName(), method.getParameterTypes());
                        break;
                    } catch (NoSuchMethodException var9) {
                        ++var6;
                    }
                }
            } catch (SecurityException var10) {
                ReflectionUtils.handleReflectionException(var10);
            } catch (NoSuchMethodException var11) {
                throw new IllegalStateException(String.format("@TccTransaction method '%s' found on bean target class '%s', but not found in any interface(s) for bean JDK proxy. Either pull the method up to an interface or switch to subclass (CGLIB) proxies by setting proxy-target-class/proxyTargetClass attribute to 'true'", method.getName(), method.getDeclaringClass().getSimpleName()));
            }
        }
        return method;
    }

}

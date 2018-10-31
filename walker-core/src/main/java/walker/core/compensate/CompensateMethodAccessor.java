package walker.core.compensate;

import com.esotericsoftware.reflectasm.MethodAccess;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CompensateMethodAccessor {

    private CompensateMethodAccessor() {
    }

    private static class MethodAccessCacheHolder {
        private static CompensateMethodAccessor instance = new CompensateMethodAccessor();
    }

    public static CompensateMethodAccessor goalkeeper() {
        return MethodAccessCacheHolder.instance;
    }

    private ConcurrentHashMap<Class<?>, MethodAccess> methodAccessCache =
            new ConcurrentHashMap<Class<?>, MethodAccess>();

    private ReentrantReadWriteLock multiReadOneWrite = new ReentrantReadWriteLock();

    public MethodAccess getWapperMethodAccess(Class<?> serviceClass) {
        multiReadOneWrite.readLock().lock();
        MethodAccess methodAccess = null;
        try {
            methodAccess = methodAccessCache.get(serviceClass);
            if (methodAccess == null) {
                multiReadOneWrite.readLock().unlock();
                multiReadOneWrite.writeLock().lock();

                try {
                    methodAccess = MethodAccess.get(serviceClass);
                } finally {
                    multiReadOneWrite.writeLock().unlock();
                }

                multiReadOneWrite.readLock().lock();
            }
        } finally {
            multiReadOneWrite.readLock().unlock();
        }
        return methodAccess;
    }

}

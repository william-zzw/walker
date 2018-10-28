package walker.core.method.compensate;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;

@Getter
@Setter
public class MethodTccCompensateEndpoint {

    private String id;
    private Object bean;
    private String tryMethodName;
    private Method tryMethod;
    private Class<?>[] tryParameterTypes;

    private Method commitMethod;
    private String commitMethodName = StringUtils.EMPTY;
    private Class<?>[] commitParameterTypes;
    private String[] commitMethodArgs;

    private Method cancelMethod;
    private String cancelMethodName =  StringUtils.EMPTY;
    private Class<?>[] cancelParameterTypes;
    private String[] cancelMethodArgs;

}

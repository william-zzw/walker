package walker.core.compensate;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;

@Setter
@Getter
public class CompensateInvoker {

    public static enum InvokerType {
        COMMIT, CANCEL
    }
    private InvokerType invokerType;
    private Object compensateService;
    private Method method;

}

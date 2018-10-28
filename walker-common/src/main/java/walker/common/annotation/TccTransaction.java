package walker.common.annotation;

import org.apache.commons.lang3.StringUtils;
import walker.common.compensate.CompensateMode;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TccTransaction {

    String id() default StringUtils.EMPTY;

    String commitMethodName() default StringUtils.EMPTY;

    String[] commitMethodArgs() default {};

    String cancelMethodName() default StringUtils.EMPTY;

    String[] cancelMethodArgs() default {};

    CompensateMode mode() default CompensateMode.SYNC;

    boolean needJudge() default false;
}
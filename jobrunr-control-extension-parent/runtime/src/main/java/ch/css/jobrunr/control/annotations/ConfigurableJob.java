package ch.css.jobrunr.control.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Inherited
@Retention(RUNTIME)
@Target(METHOD)
public @interface ConfigurableJob {

    String name() default "";

    /**
     * Indicates whether this job should be processed as a batch job.
     * <p>
     * <strong>Important:</strong> If this default value is changed, also update
     * {@code JobDefinitionIndexScanner#getBatchJobFlag(AnnotationInstance)} to maintain consistency.
     *
     * @return {@code true} if this is a batch job, {@code false} otherwise
     */
    boolean isBatch() default false;
}

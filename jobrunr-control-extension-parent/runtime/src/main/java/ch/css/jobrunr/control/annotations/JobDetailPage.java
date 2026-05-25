package ch.css.jobrunr.control.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a JobRequestHandler method as a configurable job.
 * Provides metadata for job execution settings like retries, timeouts, and resource constraints.
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target(METHOD)
public @interface JobDetailPage {

    /**
     * Defines the Recap-Parameter-Class. The Recap-Parameter Class contains the counter-Values
     * for the application specific counters.
     */
    Class<?> recapParameterClass() default Void.class;

    boolean showRecapParameterWithZeroValue() default true;

    boolean showEmptyParameters() default true;
}

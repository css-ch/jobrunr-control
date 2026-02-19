package ch.css.jobrunr.control.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a JobRequest record to use external parameter storage.
 *
 * <p>This annotation determines that the job uses EXTERNAL parameter storage.
 * Parameters are defined in the annotation's value array and stored in a separate
 * database table. The job handler retrieves parameters using the job's own UUID via
 * {@code ThreadLocalJobContext.getJobContext().getJobId()}.</p>
 *
 * <h3>Requirements:</h3>
 * <ul>
 *   <li>Must be applied to the JobRequest record type</li>
 *   <li>The value array must contain at least one JobParameterDefinition</li>
 *   <li>Each JobParameterDefinition must have both 'name' and 'type' attributes</li>
 *   <li>Hibernate ORM must be enabled for external storage to work</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>
 * {@literal @}JobParameterSet({
 *     {@literal @}JobParameterDefinition(name = "userName", type = "java.lang.String", defaultValue = "admin"),
 *     {@literal @}JobParameterDefinition(name = "count", type = "java.lang.Integer", defaultValue = "10"),
 *     {@literal @}JobParameterDefinition(name = "startDate", type = "java.time.LocalDate", defaultValue = "2024-01-01")
 * })
 * public record MyJobRequest() implements JobRequest {
 *     {@literal @}Override
 *     public Class&lt;MyJob&gt; getJobRequestHandler() { return MyJob.class; }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JobParameterSet {
    /**
     * Parameter definitions for this parameter set.
     * These define the schema of parameters stored externally.
     *
     * @return array of parameter definitions (must not be empty)
     */
    JobParameterDefinition[] value();
}

package ch.css.jobrunr.control.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a String field in a JobRequest to receive the parameter set ID
 * when using external parameter storage strategy.
 *
 * <p>This annotation determines that the job uses EXTERNAL parameter storage.
 * The annotated field will contain a UUID reference to the ParameterSetEntity.
 * Parameters are defined in the annotation's value array, not on record components.</p>
 *
 * <h3>Requirements:</h3>
 * <ul>
 *   <li>Must be applied to exactly ONE record component per JobRequest</li>
 *   <li>Field must be of type String</li>
 *   <li>The value array must contain at least one JobParameterDefinition</li>
 *   <li>Each JobParameterDefinition must have both 'name' and 'type' attributes</li>
 *   <li>Hibernate ORM must be enabled for external storage to work</li>
 * </ul>
 *
 * <h3>Example:</h3>
 * <pre>
 * public record MyJobRequest(
 *     {@literal @}JobParameterSet({
 *         {@literal @}JobParameterDefinition(name = "userName", type = "java.lang.String", defaultValue = "admin"),
 *         {@literal @}JobParameterDefinition(name = "count", type = "java.lang.Integer", defaultValue = "10"),
 *         {@literal @}JobParameterDefinition(name = "startDate", type = "java.time.LocalDate", defaultValue = "2024-01-01")
 *     })
 *     String parameterSetId
 * ) implements JobRequest {
 *     {@literal @}Override
 *     public Class&lt;MyJob&gt; getJobRequestHandler() { return MyJob.class; }
 * }
 * </pre>
 */
@Target(ElementType.RECORD_COMPONENT)
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

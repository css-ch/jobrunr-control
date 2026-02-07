package ch.css.jobrunr.control.annotations;

import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.server.BackgroundJobServerConfiguration;

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
public @interface ConfigurableJob {

    /**
     * Display name for the job.
     */
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

    /**
     * Sentinel value indicating retries not explicitly configured.
     */
    int NBR_OF_RETRIES_NOT_PROVIDED = -1;

    /**
     * Number of retry attempts for failed jobs.
     */
    int retries() default NBR_OF_RETRIES_NOT_PROVIDED;

    /**
     * The labels for the job. Parameter substitution is supported by means of <code>%0</code> syntax (this will be replaced by the toString representation of the first argument).
     *
     * @return the labels for the job.
     */
    String[] labels() default {};

    /**
     * Custom JobRunr filters to apply to this job.
     */
    Class<? extends JobFilter>[] jobFilters() default {};

    /**
     * The queue for this job. It has a maximum length of 128 characters.
     *
     * @return the queue name
     */
    String queue() default "";

    /**
     * Specifies which background job server should execute this job.
     * Maximum length: 128 characters.
     */
    String runOnServerWithTag() default BackgroundJobServerConfiguration.DEFAULT_SERVER_TAG;

    /**
     * Limits concurrency for this job. Cannot be combined with rateLimiter.
     * Maximum length: 128 characters.
     */
    String mutex() default "";

    /**
     * Specifies the rate limiter for concurrency control. Cannot be combined with mutex.
     * Maximum length: 128 characters.
     */
    String rateLimiter() default "";

    /**
     * Maximum processing duration in ISO-8601 format (e.g., PT5M for 5 minutes).
     * Job moves to FAILED state when timeout is exceeded.
     */
    String processTimeOut() default "";

    /**
     * Duration after which succeeded jobs are deleted.
     * Format: duration1!duration2 (e.g., PT5M!PT10H).
     */
    String deleteOnSuccess() default "";

    /**
     * Duration after which failed jobs are deleted.
     * Format: duration1!duration2 (e.g., PT5M!PT10H).
     */
    String deleteOnFailure() default "";
}

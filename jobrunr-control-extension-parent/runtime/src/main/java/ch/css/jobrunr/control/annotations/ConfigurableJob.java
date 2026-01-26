package ch.css.jobrunr.control.annotations;

import org.jobrunr.jobs.filters.JobFilter;
import org.jobrunr.server.BackgroundJobServerConfiguration;

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

    int NBR_OF_RETRIES_NOT_PROVIDED = -1;


    int retries() default NBR_OF_RETRIES_NOT_PROVIDED;

    /**
     * The labels for the job. Parameter substitution is supported by means of <code>%0</code> syntax (this will be replaced by the toString representation of the first argument).
     *
     * @return the labels for the job.
     */
    String[] labels() default {};

    Class<? extends JobFilter>[] jobFilters() default {};

    /**
     * The queue for this job. It has a maximum length of 128 characters.
     *
     * @return the queue name
     */
    String queue() default "";

    /**
     * Allows to filter on which background job server the job will run. The given tag must match a background job server or it will never execute! It has a maximum length of 128 characters.
     */
    String runOnServerWithTag() default BackgroundJobServerConfiguration.DEFAULT_SERVER_TAG;

    /**
     * Allows to limit concurrency for this job. It has a maximum length of 128 characters and <em>cannot</em> be combined with the rate limiter.
     */
    String mutex() default "";

    /**
     * Allows to specify the rate limiter which allows to limit concurrency for this job. It has a maximum length of 128 characters and <em>cannot</em> be combined with the rate limiter.
     */
    String rateLimiter() default "";

    /**
     * Allows to specify the maximum process duration in <a href="https://en.wikipedia.org/wiki/ISO_8601#Durations">ISO Duration format</a> after which the job will be interrupted and move to the FAILED state.
     * This time-out duration represents the time the Job is in the PROCESSING state.
     * <p>
     * An example is PT5M which means the job will be interrupted after 5 minutes
     */
    String processTimeOut() default "";

    /**
     * Allows to specify the duration after which to delete succeeded jobs in the following format:
     * <code>duration1((!)duration2)</code>
     * where
     * - duration 1 is the duration after which a succeeded job will move to the DELETED state
     * - duration 2 is the duration after which a job in the DELETED state will be permanently deleted.
     * <p>
     * An example is PT5M!PT10H which means the job will move to the DELETED state 5 minutes after succeeding and will be deleted permanently 10 hours later
     */
    String deleteOnSuccess() default "";

    /**
     * Allows to specify the duration after which to delete failed jobs in the following format:
     * <code>duration1((!)duration2)</code>
     * where
     * - duration 1 is the duration after which a failed job will move to the DELETED state
     * - duration 2 is the duration after which a job in the DELETED state will be permanently deleted.
     * <p>
     * An example is PT5M!PT10H which means the job will move to the DELETED state 5 minutes after failing and will be deleted permanently 10 hours later
     */
    String deleteOnFailure() default "";
}

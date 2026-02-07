package ch.css.jobrunr.control.domain;

import java.util.List;

/**
 * Job execution settings derived from {@code @ConfigurableJob} annotation.
 *
 * @param name               Display name for the job
 * @param isBatch            Whether this is a batch job
 * @param retries            Number of retry attempts
 * @param labels             Job labels
 * @param jobFilters         Fully qualified names of custom job filters
 * @param queue              Queue name
 * @param runOnServerWithTag Server tag constraint
 * @param mutex              Mutex for concurrency control
 * @param rateLimiter        Rate limiter name
 * @param processTimeOut     Maximum processing duration
 * @param deleteOnSuccess    Deletion schedule for successful jobs
 * @param deleteOnFailure    Deletion schedule for failed jobs
 */
public record JobSettings(
        String name,
        boolean isBatch,
        int retries,
        List<String> labels,
        List<String> jobFilters,
        String queue,
        String runOnServerWithTag,
        String mutex,
        String rateLimiter,
        String processTimeOut,
        String deleteOnSuccess,
        String deleteOnFailure
) {
}

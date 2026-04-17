package ch.css.jobrunr.control.jobs.purejobrunr;

import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * JobRequest for a job that is intentionally not registered with {@code @ConfigurableJob}.
 * Demonstrates that JobRunr Control gracefully ignores jobs it does not manage
 * (see issue #21).
 */
public record PureJobRunrJobRequest(String payload) implements JobRequest {
    @Override
    public Class<PureJobRunrJob> getJobRequestHandler() {
        return PureJobRunrJob.class;
    }
}

package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Executes a scheduled job immediately.
 * Clones the job parameters and executes the job right away.
 */
@ApplicationScoped
public class ExecuteJobImmediatelyUseCase {

    private final JobSchedulerPort jobSchedulerPort;

    @Inject
    public ExecuteJobImmediatelyUseCase(JobSchedulerPort jobSchedulerPort) {
        this.jobSchedulerPort = jobSchedulerPort;
    }

    /**
     * Executes a scheduled job immediately.
     *
     * @param jobId ID of the job to execute
     */
    public void execute(UUID jobId) {
        execute(jobId, null);
    }

    /**
     * Executes a scheduled job immediately with parameter overrides.
     *
     * @param jobId    ID of the job to execute
     * @param metadata Additional metadata
     */
    public void execute(UUID jobId, java.util.Map<String, Object> metadata) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        jobSchedulerPort.executeJobNow(jobId, metadata);
    }
}


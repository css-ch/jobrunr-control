package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Deletes a scheduled job.
 * With confirmation logic.
 */
@ApplicationScoped
public class DeleteScheduledJobUseCase {

    private final JobSchedulerPort jobSchedulerPort;

    @Inject
    public DeleteScheduledJobUseCase(JobSchedulerPort jobSchedulerPort) {
        this.jobSchedulerPort = jobSchedulerPort;
    }

    /**
     * Deletes a scheduled job.
     *
     * @param jobId ID of the job to delete
     */
    public void execute(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        jobSchedulerPort.deleteScheduledJob(jobId);
    }

    /**
     * Deletes a scheduled job with confirmation.
     *
     * @param jobId     ID of the job to delete
     * @param confirmed Confirmation of the deletion
     */
    public void execute(UUID jobId, boolean confirmed) {
        if (!confirmed) {
            throw new IllegalStateException("Deletion was not confirmed");
        }

        execute(jobId);
    }
}


package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use Case: Deletes a scheduled job.
 * With confirmation logic.
 */
@ApplicationScoped
public class DeleteScheduledJobUseCase {

    private static final Logger log = Logger.getLogger(DeleteScheduledJobUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final DeleteParametersUseCase deleteParametersUseCase;

    @Inject
    public DeleteScheduledJobUseCase(
            JobSchedulerPort jobSchedulerPort,
            DeleteParametersUseCase deleteParametersUseCase) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.deleteParametersUseCase = deleteParametersUseCase;
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

        // Before deleting job, check if it has external parameters and clean them up
        try {
            ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);
            if (jobInfo != null && jobInfo.hasExternalParameters()) {
                jobInfo.getParameterSetId().ifPresent(paramSetId -> {
                    log.debugf("Cleaning up external parameters for job %s: %s", jobId, paramSetId);
                    deleteParametersUseCase.execute(paramSetId);
                });
            }
        } catch (Exception e) {
            log.warnf("Failed to cleanup external parameters for job %s: %s", jobId, e.getMessage());
            // Continue with job deletion even if parameter cleanup fails
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


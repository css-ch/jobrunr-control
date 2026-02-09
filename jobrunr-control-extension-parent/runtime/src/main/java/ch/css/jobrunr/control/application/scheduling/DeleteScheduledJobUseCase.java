package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
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

    private static final Logger LOG = Logger.getLogger(DeleteScheduledJobUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final ParameterStoragePort parameterStoragePort;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public DeleteScheduledJobUseCase(
            JobSchedulerPort jobSchedulerPort,
            ParameterStoragePort parameterStoragePort,
            AuditLoggerHelper auditLogger) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStoragePort = parameterStoragePort;
        this.auditLogger = auditLogger;
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

        // Get job info before deletion for audit logging
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);
        String jobName = jobInfo != null ? jobInfo.getJobName() : "Unknown";

        // Before deleting job, check if it has external parameters and clean them up
        try {
            if (jobInfo != null && jobInfo.hasExternalParameters()) {
                jobInfo.getParameterSetId().ifPresent(paramSetId -> {
                    LOG.debugf("Cleaning up external parameters for job %s: %s", jobId, paramSetId);
                    parameterStoragePort.deleteById(paramSetId);
                    LOG.infof("Deleted parameter set: %s", paramSetId);
                });
            }
        } catch (Exception e) {
            LOG.warnf("Failed to cleanup external parameters for job %s: %s", jobId, e.getMessage());
            // Continue with job deletion even if parameter cleanup fails
        }

        jobSchedulerPort.deleteScheduledJob(jobId);

        // Audit log
        auditLogger.logJobDeleted(jobName, jobId);
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


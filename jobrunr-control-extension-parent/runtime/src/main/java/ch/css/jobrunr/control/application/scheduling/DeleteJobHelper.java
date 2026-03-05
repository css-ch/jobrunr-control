package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Helper that encapsulates the shared deletion logic for scheduled jobs and templates:
 * optional parameter-set cleanup, job deletion, and audit logging.
 */
@ApplicationScoped
public class DeleteJobHelper {

    private static final Logger LOG = Logger.getLogger(DeleteJobHelper.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public DeleteJobHelper(JobSchedulerPort jobSchedulerPort, ParameterStoragePort parameterStoragePort) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStoragePort = parameterStoragePort;
    }

    /**
     * Deletes a job, cleans up any associated external parameter set, and calls the provided audit action.
     *
     * @param jobId       ID of the job to delete
     * @param auditAction callback that receives (jobName, jobId) for audit logging
     */
    public void deleteJobWithCleanup(UUID jobId, BiConsumer<String, UUID> auditAction) {
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);
        String jobName = jobInfo != null ? jobInfo.getJobName() : "Unknown";

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
        auditAction.accept(jobName, jobId);
    }
}

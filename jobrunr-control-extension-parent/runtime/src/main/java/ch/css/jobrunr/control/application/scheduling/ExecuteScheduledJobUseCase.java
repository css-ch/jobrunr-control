package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Executes a scheduled job immediately.
 * This use case is for executing regular scheduled jobs (not template jobs).
 * Template jobs must be executed using ExecuteTemplateUseCase instead.
 */
@ApplicationScoped
public class ExecuteScheduledJobUseCase {

    private final JobSchedulerPort jobSchedulerPort;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public ExecuteScheduledJobUseCase(JobSchedulerPort jobSchedulerPort, AuditLoggerHelper auditLogger) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.auditLogger = auditLogger;
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

        // Get job info for audit logging
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);
        String jobName = jobInfo != null ? jobInfo.getJobName() : "Unknown";

        jobSchedulerPort.executeJobNow(jobId, metadata);

        // Audit log - this is always called from UI
        auditLogger.logJobExecutedViaUI(jobName, jobId);
    }
}


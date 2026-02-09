package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use Case: Deletes a template job.
 */
@ApplicationScoped
public class DeleteTemplateUseCase {

    private static final Logger LOG = Logger.getLogger(DeleteTemplateUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final ParameterStoragePort parameterStoragePort;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public DeleteTemplateUseCase(
            JobSchedulerPort jobSchedulerPort,
            ParameterStoragePort parameterStoragePort,
            AuditLoggerHelper auditLogger) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStoragePort = parameterStoragePort;
        this.auditLogger = auditLogger;
    }

    /**
     * Deletes a template job.
     *
     * @param templateId Template job ID
     */
    public void execute(UUID templateId) {
        if (templateId == null) {
            throw new IllegalArgumentException("templateId must not be null");
        }

        // Get job info before deletion for audit logging
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(templateId);
        String templateName = jobInfo != null ? jobInfo.getJobName() : "Unknown";

        // Before deleting job, check if it has external parameters and clean them up
        try {
            if (jobInfo != null && jobInfo.hasExternalParameters()) {
                jobInfo.getParameterSetId().ifPresent(paramSetId -> {
                    LOG.debugf("Cleaning up external parameters for template %s: %s", templateId, paramSetId);
                    parameterStoragePort.deleteById(paramSetId);
                    LOG.infof("Deleted parameter set: %s", paramSetId);
                });
            }
        } catch (Exception e) {
            LOG.warnf("Failed to cleanup external parameters for template %s: %s", templateId, e.getMessage());
            // Continue with job deletion even if parameter cleanup fails
        }

        jobSchedulerPort.deleteScheduledJob(templateId);

        // Audit log
        auditLogger.logTemplateDeleted(templateName, templateId);
    }
}

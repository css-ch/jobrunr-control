package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.scheduling.DeleteJobHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Deletes a template job.
 */
@ApplicationScoped
public class DeleteTemplateUseCase {

    private final DeleteJobHelper deleteJobHelper;
    private final AuditLoggerHelper auditLogger;

    @Inject
    public DeleteTemplateUseCase(DeleteJobHelper deleteJobHelper, AuditLoggerHelper auditLogger) {
        this.deleteJobHelper = deleteJobHelper;
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
        deleteJobHelper.deleteJobWithCleanup(templateId, auditLogger::logTemplateDeleted);
    }
}

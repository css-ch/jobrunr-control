package ch.css.jobrunr.control.application.audit;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralized audit logger for Template and Job operations.
 * Provides consistent audit log format across the application.
 * <p>
 * Log format: AUDIT [user] [action] [entity-type] [name] [id] [additional-details]
 * </p>
 * <p>
 * Examples:
 * - AUDIT user1 created template MyTemplate 123e4567-e89b-12d3-a456-426614174000 with parameters {param1=value1, param2=value2}
 * - AUDIT user2 updated job MyJob 123e4567-e89b-12d3-a456-426614174000 with parameters {param1=newValue}
 * - AUDIT user3 deleted template OldTemplate 123e4567-e89b-12d3-a456-426614174000
 * - AUDIT user4 executed template (via REST) MyTemplate 123e4567-e89b-12d3-a456-426614174000 -> job 987e6543-e21b-12d3-a456-426614174999
 * - AUDIT user5 executed template (via UI) MyTemplate 123e4567-e89b-12d3-a456-426614174000 -> job 987e6543-e21b-12d3-a456-426614174999
 * </p>
 */
@ApplicationScoped
public class AuditLoggerHelper {

    private static final Logger LOG = Logger.getLogger(AuditLoggerHelper.class);
    private static final String AUDIT_PREFIX = "AUDIT";

    private final SecurityIdentity securityIdentity;

    @Inject
    public AuditLoggerHelper(SecurityIdentity securityIdentity) {
        this.securityIdentity = securityIdentity;
    }

    /**
     * Logs template creation.
     *
     * @param templateName Template name
     * @param templateId   Template ID
     * @param parameters   Template parameters
     */
    public void logTemplateCreated(String templateName, UUID templateId, Map<String, Object> parameters) {
        String user = getCurrentUser();
        String paramsStr = formatParameters(parameters);
        LOG.infof("%s %s created template %s %s with parameters %s",
                AUDIT_PREFIX, user, templateName, templateId, paramsStr);
    }

    /**
     * Logs template update.
     *
     * @param templateName Template name
     * @param templateId   Template ID
     * @param parameters   Updated parameters
     */
    public void logTemplateUpdated(String templateName, UUID templateId, Map<String, Object> parameters) {
        String user = getCurrentUser();
        String paramsStr = formatParameters(parameters);
        LOG.infof("%s %s updated template %s %s with parameters %s",
                AUDIT_PREFIX, user, templateName, templateId, paramsStr);
    }

    /**
     * Logs template deletion.
     *
     * @param templateName Template name
     * @param templateId   Template ID
     */
    public void logTemplateDeleted(String templateName, UUID templateId) {
        String user = getCurrentUser();
        LOG.infof("%s %s deleted template %s %s",
                AUDIT_PREFIX, user, templateName, templateId);
    }

    /**
     * Logs template execution via REST API.
     *
     * @param templateName  Template name
     * @param templateId    Template ID
     * @param executedJobId ID of the executed job
     */
    public void logTemplateExecutedViaRest(String templateName, UUID templateId, UUID executedJobId) {
        String user = getCurrentUser();
        LOG.infof("%s %s executed template (via REST) %s %s -> job %s",
                AUDIT_PREFIX, user, templateName, templateId, executedJobId);
    }

    /**
     * Logs template execution via UI.
     *
     * @param templateName  Template name
     * @param templateId    Template ID
     * @param executedJobId ID of the executed job
     */
    public void logTemplateExecutedViaUI(String templateName, UUID templateId, UUID executedJobId) {
        String user = getCurrentUser();
        LOG.infof("%s %s executed template (via UI) %s %s -> job %s",
                AUDIT_PREFIX, user, templateName, templateId, executedJobId);
    }

    /**
     * Logs job creation.
     *
     * @param jobName    Job name
     * @param jobId      Job ID
     * @param parameters Job parameters
     */
    public void logJobCreated(String jobName, UUID jobId, Map<String, Object> parameters) {
        String user = getCurrentUser();
        String paramsStr = formatParameters(parameters);
        LOG.infof("%s %s created job %s %s with parameters %s",
                AUDIT_PREFIX, user, jobName, jobId, paramsStr);
    }

    /**
     * Logs job update.
     *
     * @param jobName    Job name
     * @param jobId      Job ID
     * @param parameters Updated parameters
     */
    public void logJobUpdated(String jobName, UUID jobId, Map<String, Object> parameters) {
        String user = getCurrentUser();
        String paramsStr = formatParameters(parameters);
        LOG.infof("%s %s updated job %s %s with parameters %s",
                AUDIT_PREFIX, user, jobName, jobId, paramsStr);
    }

    /**
     * Logs job deletion.
     *
     * @param jobName Job name
     * @param jobId   Job ID
     */
    public void logJobDeleted(String jobName, UUID jobId) {
        String user = getCurrentUser();
        LOG.infof("%s %s deleted job %s %s",
                AUDIT_PREFIX, user, jobName, jobId);
    }

    /**
     * Logs job execution via REST API.
     *
     * @param jobName Job name
     * @param jobId   Job ID
     */
    public void logJobExecutedViaRest(String jobName, UUID jobId) {
        String user = getCurrentUser();
        LOG.infof("%s %s executed job (via REST) %s %s",
                AUDIT_PREFIX, user, jobName, jobId);
    }

    /**
     * Logs job execution via UI.
     *
     * @param jobName Job name
     * @param jobId   Job ID
     */
    public void logJobExecutedViaUI(String jobName, UUID jobId) {
        String user = getCurrentUser();
        LOG.infof("%s %s executed job (via UI) %s %s",
                AUDIT_PREFIX, user, jobName, jobId);
    }

    /**
     * Gets the current user from security context.
     *
     * @return Username or "anonymous" if not authenticated
     */
    private String getCurrentUser() {
        if (securityIdentity == null || securityIdentity.isAnonymous()) {
            return "anonymous";
        }
        return securityIdentity.getPrincipal().getName();
    }

    /**
     * Formats parameters map for logging.
     *
     * @param parameters Parameters map
     * @return Formatted string representation
     */
    private String formatParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "{}";
        }
        return parameters.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}


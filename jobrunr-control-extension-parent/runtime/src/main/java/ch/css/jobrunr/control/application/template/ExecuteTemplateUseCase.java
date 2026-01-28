package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Executes a template job by cloning it and starting the clone immediately.
 * Template jobs cannot be executed directly and must be cloned first.
 */
@ApplicationScoped
public class ExecuteTemplateUseCase {

    private static final Logger log = Logger.getLogger(ExecuteTemplateUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final TemplateCloneHelper templateCloneHelper;

    @Inject
    public ExecuteTemplateUseCase(
            JobSchedulerPort jobSchedulerPort,
            TemplateCloneHelper templateCloneHelper) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.templateCloneHelper = templateCloneHelper;
    }

    /**
     * Executes a template job by cloning it and starting the clone immediately with optional parameter overrides.
     *
     * @param templateId         ID of the template job to execute
     * @param postfix            Optional postfix for the cloned job name (defaults to current date in yyyyMMdd format)
     * @param parameterOverrides Optional parameters to override in the cloned job
     * @return UUID of the newly created and started job
     * @throws IllegalArgumentException if the template job is not found
     */
    public UUID execute(UUID templateId, String postfix, Map<String, Object> parameterOverrides) {
        // Clone the template job (without "template" label, as this is an executable job)
        UUID newJobId = templateCloneHelper.cloneTemplate(
                templateId,
                postfix,
                parameterOverrides,
                null  // No additional labels - this is not a template, it's an executable job
        );

        // Start the job immediately
        jobSchedulerPort.executeJobNow(newJobId, parameterOverrides);

        log.infof("Started cloned job %s", newJobId);

        return newJobId;
    }
}

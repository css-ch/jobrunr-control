package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
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

    @Inject
    public DeleteTemplateUseCase(
            JobSchedulerPort jobSchedulerPort,
            ParameterStoragePort parameterStoragePort) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.parameterStoragePort = parameterStoragePort;
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

        // Before deleting job, check if it has external parameters and clean them up
        try {
            ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(templateId);
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
    }
}

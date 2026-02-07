package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.template.TemplateCloneHelper;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Starts a job (regular or template).
 * This use case determines whether the job is a template or a regular scheduled job
 * and executes the appropriate logic using domain ports.
 */
@ApplicationScoped
public class StartJobUseCase {

    private static final Logger LOG = Logger.getLogger(StartJobUseCase.class);

    private final JobSchedulerPort jobSchedulerPort;
    private final TemplateCloneHelper templateCloneHelper;

    @Inject
    public StartJobUseCase(
            JobSchedulerPort jobSchedulerPort,
            TemplateCloneHelper templateCloneHelper) {
        this.jobSchedulerPort = jobSchedulerPort;
        this.templateCloneHelper = templateCloneHelper;
    }

    /**
     * Starts a job. If the job is a template, it will be cloned and started.
     * If it's a regular job, it will be started directly.
     *
     * @param jobId              ID of the job to start
     * @param postfix            Optional postfix for template job names (ignored for regular jobs)
     * @param parameterOverrides Optional parameters to override
     * @return UUID of the started job (same as input for regular jobs, new UUID for templates)
     * @throws NotFoundException if the job is not found
     */
    public UUID execute(UUID jobId, String postfix, Map<String, Object> parameterOverrides) {
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);

        if (jobInfo == null) {
            throw new NotFoundException("Job with ID " + jobId + " not found");
        }

        if (jobInfo.isTemplate()) {
            LOG.infof("Job %s is a template, cloning and starting", jobId);

            // Clone the template job (without "template" label, as this is an executable job)
            UUID newJobId = templateCloneHelper.cloneTemplate(
                    jobId,
                    postfix,
                    parameterOverrides,
                    null  // No additional labels - this is not a template, it's an executable job
            );

            // Start the job immediately
            jobSchedulerPort.executeJobNow(newJobId, parameterOverrides);

            LOG.infof("Started cloned job %s from template %s", newJobId, jobId);
            return newJobId;
        } else {
            LOG.infof("Job %s is a regular scheduled job, starting directly", jobId);
            jobSchedulerPort.executeJobNow(jobId, parameterOverrides);
            return jobId;
        }
    }
}

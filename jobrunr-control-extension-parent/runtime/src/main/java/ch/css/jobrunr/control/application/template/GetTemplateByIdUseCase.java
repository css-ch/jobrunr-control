package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Retrieves a single template job by ID.
 */
@ApplicationScoped
public class GetTemplateByIdUseCase {

    private final JobSchedulerPort jobSchedulerPort;

    @Inject
    public GetTemplateByIdUseCase(JobSchedulerPort jobSchedulerPort) {
        this.jobSchedulerPort = jobSchedulerPort;
    }

    /**
     * Retrieves a template job by its ID.
     *
     * @param templateId The template job ID
     * @return Optional with ScheduledJobInfo or empty if not found or not a template
     */
    public Optional<ScheduledJobInfo> execute(UUID templateId) {
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(templateId);
        return Optional.ofNullable(jobInfo)
                .filter(ScheduledJobInfo::isTemplate);
    }
}

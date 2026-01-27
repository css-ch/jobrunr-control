package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use Case: Retrieves all template jobs.
 * Template jobs are identified by the "template" label.
 */
@ApplicationScoped
public class GetTemplatesUseCase {

    private final GetScheduledJobsUseCase getScheduledJobsUseCase;

    @Inject
    public GetTemplatesUseCase(GetScheduledJobsUseCase getScheduledJobsUseCase) {
        this.getScheduledJobsUseCase = getScheduledJobsUseCase;
    }

    /**
     * Returns all template jobs.
     *
     * @return List of template jobs
     */
    public List<ScheduledJobInfo> execute() {
        return getScheduledJobsUseCase.execute().stream()
                .filter(ScheduledJobInfo::isTemplate)
                .toList();
    }
}

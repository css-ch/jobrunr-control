package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.GetScheduledJobByIdUseCase;
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

    private final GetScheduledJobByIdUseCase getScheduledJobByIdUseCase;

    @Inject
    public GetTemplateByIdUseCase(GetScheduledJobByIdUseCase getScheduledJobByIdUseCase) {
        this.getScheduledJobByIdUseCase = getScheduledJobByIdUseCase;
    }

    /**
     * Retrieves a template job by its ID.
     *
     * @param templateId The template job ID
     * @return Optional with ScheduledJobInfo or empty if not found or not a template
     */
    public Optional<ScheduledJobInfo> execute(UUID templateId) {
        return getScheduledJobByIdUseCase.execute(templateId)
                .filter(ScheduledJobInfo::isTemplate);
    }
}

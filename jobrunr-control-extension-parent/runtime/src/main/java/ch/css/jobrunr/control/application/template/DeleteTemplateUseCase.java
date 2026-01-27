package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.DeleteScheduledJobUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Deletes a template job.
 */
@ApplicationScoped
public class DeleteTemplateUseCase {

    private final DeleteScheduledJobUseCase deleteScheduledJobUseCase;

    @Inject
    public DeleteTemplateUseCase(DeleteScheduledJobUseCase deleteScheduledJobUseCase) {
        this.deleteScheduledJobUseCase = deleteScheduledJobUseCase;
    }

    /**
     * Deletes a template job.
     *
     * @param templateId Template job ID
     */
    public void execute(UUID templateId) {
        deleteScheduledJobUseCase.execute(templateId);
    }
}

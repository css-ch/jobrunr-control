package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.UpdateScheduledJobUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Updates an existing template job.
 * Template jobs are always updated with external trigger and the "template" label.
 */
@ApplicationScoped
public class UpdateTemplateUseCase {

    private final UpdateScheduledJobUseCase updateScheduledJobUseCase;

    @Inject
    public UpdateTemplateUseCase(UpdateScheduledJobUseCase updateScheduledJobUseCase) {
        this.updateScheduledJobUseCase = updateScheduledJobUseCase;
    }

    /**
     * Updates an existing template job.
     *
     * @param templateId Template job ID
     * @param jobType    Name of the job definition (e.g., fully qualified class name)
     * @param jobName    User-defined name for this template
     * @param parameters Parameter map for job execution
     */
    public void execute(UUID templateId, String jobType, String jobName, Map<String, String> parameters) {
        // Template jobs are always external trigger with no scheduled time and maintain the "template" label
        updateScheduledJobUseCase.execute(
                templateId,
                jobType,
                jobName,
                parameters,
                null,           // scheduledAt - templates have no schedule
                true,           // isExternalTrigger
                List.of("template")  // labels
        );
    }
}

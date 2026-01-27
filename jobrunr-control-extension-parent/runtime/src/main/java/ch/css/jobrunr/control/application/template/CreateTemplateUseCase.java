package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.CreateScheduledJobUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Creates a new template job.
 * Template jobs are always created with external trigger and the "template" label.
 */
@ApplicationScoped
public class CreateTemplateUseCase {

    private final CreateScheduledJobUseCase createScheduledJobUseCase;

    @Inject
    public CreateTemplateUseCase(CreateScheduledJobUseCase createScheduledJobUseCase) {
        this.createScheduledJobUseCase = createScheduledJobUseCase;
    }

    /**
     * Creates a new template job.
     *
     * @param jobType    Name of the job definition (e.g., fully qualified class name)
     * @param jobName    User-defined name for this template
     * @param parameters Parameter map for job execution
     * @return UUID of the created template job
     */
    public UUID execute(String jobType, String jobName, Map<String, String> parameters) {
        // Template jobs are always external trigger with no scheduled time and have the "template" label
        return createScheduledJobUseCase.execute(
                jobType,
                jobName,
                parameters,
                null,           // scheduledAt - templates have no schedule
                true,           // isExternalTrigger
                List.of("template")  // labels
        );
    }
}

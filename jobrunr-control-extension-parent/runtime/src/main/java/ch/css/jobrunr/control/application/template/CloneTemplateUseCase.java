package ch.css.jobrunr.control.application.template;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Use Case: Clones a template job to create a new template.
 * The cloned template is an independent copy with a new name.
 */
@ApplicationScoped
public class CloneTemplateUseCase {

    private final TemplateCloneHelper templateCloneHelper;

    @Inject
    public CloneTemplateUseCase(TemplateCloneHelper templateCloneHelper) {
        this.templateCloneHelper = templateCloneHelper;
    }

    /**
     * Clones a template job to create a new template with the same parameters.
     *
     * @param templateId ID of the template job to clone
     * @param postfix    Optional postfix for the cloned template name (defaults to current date in yyyyMMdd format)
     * @return UUID of the newly created template
     * @throws IllegalArgumentException if the template job is not found
     */
    public UUID execute(UUID templateId, String postfix) {
        return templateCloneHelper.cloneTemplate(
                templateId,
                postfix,
                null,                   // No parameter overrides for template clones
                List.of("template")     // Ensure clone is also a template
        );
    }
}

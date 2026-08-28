package ch.css.jobrunr.control.adapter.ui.shell;

import java.util.Optional;

/**
 * Branding information for the UI shell containing JobRunr Control pages.
 *
 * @param applicationName name displayed in the shell header
 * @param homeHref link used by the shell brand
 * @param icon Bootstrap icon name without the {@code bi-} prefix
 * @param stage optional deployment stage
 */
public record JobRunrControlUiBranding(
        String applicationName,
        String homeHref,
        String icon,
        Optional<JobRunrControlUiStage> stage) {

    public JobRunrControlUiBranding {
        stage = stage == null ? Optional.empty() : stage;
    }
}

package ch.css.jobrunr.control.adapter.ui.shell;

import java.util.Optional;

/**
 * Supplies the branding for the UI shell around JobRunr Control pages.
 * Applications embedding the pages can provide their own CDI implementation.
 */
public interface JobRunrControlUiBrandingProvider {

    Optional<JobRunrControlUiBranding> branding();
}

package ch.css.jobrunr.control.example.ui;

import ch.css.jobrunr.control.adapter.ui.shell.JobRunrControlUiBranding;
import ch.css.jobrunr.control.adapter.ui.shell.JobRunrControlUiBrandingProvider;
import ch.css.jobrunr.control.adapter.ui.shell.JobRunrControlUiStage;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

/**
 * Example host-application branding enabled with the {@code ui-extensions} profile.
 */
@ApplicationScoped
@IfBuildProfile("ui-extensions")
public class ExampleUiBrandingProvider implements JobRunrControlUiBrandingProvider {

    @Override
    public Optional<JobRunrControlUiBranding> branding() {
        return Optional.of(new JobRunrControlUiBranding(
                "Print Dashboard",
                "/api/hello",
                "printer",
                Optional.of(new JobRunrControlUiStage("DEV", "stage-dev,red", "beaker"))));
    }
}

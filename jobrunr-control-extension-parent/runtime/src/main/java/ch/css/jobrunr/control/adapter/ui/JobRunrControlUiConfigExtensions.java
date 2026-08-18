package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.qute.TemplateExtension;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

@TemplateExtension(namespace = "jobrunrControlUiConfig")
public class JobRunrControlUiConfigExtensions {

    private JobRunrControlUiConfigExtensions() {
        // Utility class - prevent instantiation
    }

    /**
     * Whether the scheduled jobs menu entry should be displayed.
     *
     * @return {@code true} if scheduled jobs should be shown, otherwise {@code false}
     */
    @SuppressWarnings("unused") // Used by Qute templates
    public static boolean showScheduledJobs() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("quarkus.jobrunr-control.ui.show-scheduled-jobs", Boolean.class)
                .orElse(true);
    }
}

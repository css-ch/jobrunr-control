package ch.css.jobrunr.control.adapter.ui;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Service for generating JobRunr Dashboard URLs based on configuration.
 * Supports both embedded and standalone dashboard types.
 */
@ApplicationScoped
public class DashboardUrlUtils {

    private final String dashboardType;
    private final String contextPath;
    private final Optional<Integer> dashboardPort;

    public DashboardUrlUtils(
            @ConfigProperty(name = "quarkus.jobrunr.dashboard.type", defaultValue = "embedded") String dashboardType,
            @ConfigProperty(name = "quarkus.jobrunr.dashboard.context-path", defaultValue = "/dashboard") String contextPath,
            @ConfigProperty(name = "quarkus.jobrunr.dashboard.port") Optional<Integer> dashboardPort) {
        this.dashboardType = dashboardType;
        this.contextPath = contextPath + "/dashboard";
        this.dashboardPort = dashboardPort;
    }

    /**
     * Get the base URL for the JobRunr Dashboard.
     *
     * @return The base URL (e.g., "http://localhost:8000" for standalone, "" for embedded)
     */
    public String getDashboardBaseUrl() {
        if ("embedded".equalsIgnoreCase(dashboardType)) {
            return "";
        } else {
            int port = dashboardPort.orElse(8000);
            return "http://localhost:" + port;
        }
    }

    /**
     * Get the context path for the JobRunr Dashboard.
     *
     * @return The context path (e.g., "/dashboard" or "/my-custom-dashboard")
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Get the full URL to a specific job in the dashboard.
     *
     * @param jobId The job ID
     * @return The full URL to the job
     */
    public String getJobUrl(String jobId) {
        return getDashboardBaseUrl() + contextPath + "/jobs/" + jobId;
    }

    /**
     * Get the full URL to jobs filtered by label/type.
     *
     * @param label The job type/label
     * @return The full URL to filtered jobs
     */
    public String getJobsByLabelUrl(String label) {
        return getDashboardBaseUrl() + contextPath + "/jobs?label=" + label;
    }

    /**
     * Get the full URL to the dashboard root.
     *
     * @return The full URL to the dashboard
     */
    public String getDashboardUrl() {
        return getDashboardBaseUrl() + contextPath;
    }

    /**
     * Check if the dashboard is embedded.
     *
     * @return true if embedded, false otherwise
     */
    public boolean isEmbedded() {
        return "embedded".equalsIgnoreCase(dashboardType);
    }
}

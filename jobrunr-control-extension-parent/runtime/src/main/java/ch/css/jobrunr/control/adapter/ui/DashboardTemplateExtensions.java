package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.domain.BuildTimeConfigurationPort;
import io.quarkus.arc.Arc;
import io.quarkus.qute.TemplateData;
import io.quarkus.qute.TemplateGlobal;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Producer for dashboard template data.
 * Makes dashboard URL methods available in all Qute templates via {dashboard}.
 */
@ApplicationScoped
@SuppressWarnings("unused") // Methods are used in Qute templates
public class DashboardTemplateExtensions {

    /**
     * Produces a Dashboard object for use in Qute templates.
     * Available in templates as {dashboard}.
     */
    @TemplateGlobal
    public static Dashboard dashboard() {
        DashboardUrlUtils dashboardUrlService = Arc.container().instance(DashboardUrlUtils.class).get();
        BuildTimeConfigurationPort buildConfigPort = Arc.container().instance(BuildTimeConfigurationPort.class).get();
        return new Dashboard(dashboardUrlService, buildConfigPort);
    }

    /**
     * Dashboard helper class that provides URL methods in type-safe templates.
     * Usage:
     * - {dashboard.url()} - Dashboard root URL
     * - {dashboard.jobUrl(jobId)} - Job details URL
     * - {dashboard.jobsByLabelUrl(label)} - Jobs by type URL
     */
    @TemplateData
    @SuppressWarnings("unused") // Methods are used in type-safe Qute templates
    public static class Dashboard {
        private final DashboardUrlUtils service;
        private final BuildTimeConfigurationPort buildConfigPort;
        private final String version;

        public Dashboard(DashboardUrlUtils service, BuildTimeConfigurationPort buildConfigPort) {
            this.service = service;
            this.buildConfigPort = buildConfigPort;
            this.version = loadVersion();
        }

        private String loadVersion() {
            try (var is = getClass().getResourceAsStream("/jobrunr-control-version.properties")) {
                if (is != null) {
                    var props = new java.util.Properties();
                    props.load(is);
                    return props.getProperty("version", "unknown");
                }
            } catch (Exception e) {
                // ignore
            }
            return "unknown";
        }

        public String version() {
            return version;
        }

        public String url() {
            return service.getDashboardUrl();
        }

        public String jobUrl(Object jobId) {
            return service.getJobUrl(String.valueOf(jobId));
        }

        public String jobsByLabelUrl(String label) {
            return service.getJobsByLabelUrl(label);
        }

        public boolean hasOpenApi() {
            return buildConfigPort.isOpenApiAvailable();
        }
    }
}

package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
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
public class ResultPageUrlTemplateExtensions {

    /**
     * Produces a Dashboard object for use in Qute templates.
     * Available in templates as {dashboard}.
     */
    @TemplateGlobal
    public static ResultPageUrl resultPageUrl() {
        ResultPageUrlUtils resultPageUrlUtils = Arc.container().instance(ResultPageUrlUtils.class).get();
        return new ResultPageUrl(resultPageUrlUtils);
    }

    /**
     * Produces a DetailPage object for use in Qute templates.
     * Available in templates as {dashboard}.
     */
    @TemplateGlobal
    public static DetailPage hasDetailPage() {
        DetailPageUtils detailPageUtils = Arc.container().instance(DetailPageUtils.class).get();
        return new DetailPage(detailPageUtils);
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
    public static class ResultPageUrl {
        private final ResultPageUrlUtils service;

        public ResultPageUrl(ResultPageUrlUtils service) {
            this.service = service;
        }

        public String resultPageUrl(JobExecutionInfo job, String host, String port) {
            return service.getResultPageUrl(job, host, port);
        }
    }

    /**
     * DetailPage helper class that provides if the detail-page is enabled
     */
    @TemplateData
    @SuppressWarnings("unused") // Methods are used in type-safe Qute templates
    public static class DetailPage {
        private final DetailPageUtils service;

        public DetailPage(DetailPageUtils service) {
            this.service = service;
        }

        public boolean hasDetailPage(String jobType) {
            return service.hasDetailPage(jobType);
        }
    }
}

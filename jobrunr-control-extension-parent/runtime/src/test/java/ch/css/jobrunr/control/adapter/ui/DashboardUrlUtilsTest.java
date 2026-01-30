package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DashboardUrlUtilsTest {

    @Test
    void shouldGenerateEmbeddedDashboardUrls() {
        DashboardUrlUtils utils = new DashboardUrlUtils("embedded", "/app", Optional.empty());

        assertEquals("", utils.getDashboardBaseUrl());
        assertEquals("/app/dashboard", utils.getContextPath());
        assertEquals("/app/dashboard", utils.getDashboardUrl());
        assertTrue(utils.isEmbedded());
    }

    @Test
    void shouldGenerateStandaloneDashboardUrlsWithDefaultPort() {
        DashboardUrlUtils utils = new DashboardUrlUtils("standalone", "/custom", Optional.empty());

        assertEquals("http://localhost:8000", utils.getDashboardBaseUrl());
        assertEquals("/custom/dashboard", utils.getContextPath());
        assertEquals("http://localhost:8000/custom/dashboard", utils.getDashboardUrl());
        assertFalse(utils.isEmbedded());
    }

    @Test
    void shouldGenerateStandaloneDashboardUrlsWithCustomPort() {
        DashboardUrlUtils utils = new DashboardUrlUtils("standalone", "/monitoring", Optional.of(9090));

        assertEquals("http://localhost:9090", utils.getDashboardBaseUrl());
        assertEquals("/monitoring/dashboard", utils.getContextPath());
        assertEquals("http://localhost:9090/monitoring/dashboard", utils.getDashboardUrl());
        assertFalse(utils.isEmbedded());
    }

    @Test
    void shouldGenerateJobUrl() {
        DashboardUrlUtils utils = new DashboardUrlUtils("embedded", "/app", Optional.empty());

        String jobUrl = utils.getJobUrl("job-123-abc");

        assertEquals("/app/dashboard/jobs/job-123-abc", jobUrl);
    }

    @Test
    void shouldGenerateJobsByLabelUrl() {
        DashboardUrlUtils utils = new DashboardUrlUtils("embedded", "/app", Optional.empty());

        String labelUrl = utils.getJobsByLabelUrl("BatchJob");

        assertEquals("/app/dashboard/jobs?label=BatchJob", labelUrl);
    }
}

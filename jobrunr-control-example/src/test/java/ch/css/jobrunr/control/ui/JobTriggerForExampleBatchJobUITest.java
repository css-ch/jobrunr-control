package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI test for creating an ExampleBatchJob with external trigger and verifying
 * execution in history.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobTriggerForExampleBatchJobUITest extends JobTriggerUITestBase {

    @Test
    @Order(1)
    @DisplayName("Create a batch job with external trigger via UI")
    void testCreateJobWithExternalTrigger() {
        navigateToScheduledJobsPage();
        openJobCreationDialog();
        selectJobType("ExampleBatchJob");
        fillJobName("Test Batch Job - External Trigger");
        fillBatchJobParametersWithDefaults();
        enableExternalTrigger();
        submitJobCreationForm();

        scheduledJobId = extractJobIdFromScheduledJobsTable("Test Batch Job - External Trigger");
        assertNotNull(scheduledJobId, "Scheduled job should be created and its ID extracted from the table");
    }

    @Test
    @Order(2)
    @DisplayName("Trigger the batch job via REST API")
    void testTriggerJobViaRest() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        String response = triggerJobViaApi(scheduledJobId);
        assertTrue(response.contains("Job started successfully"), "Job should be started successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Check batch job execution in history")
    void testCheckJobExecutionInHistory() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToHistory();
        searchForJob("Test Batch Job - External Trigger");
        verifyJobInHistory("Test Batch Job - External Trigger", "Example Batch Job");
    }

    @Test
    @Order(4)
    @DisplayName("Verify batch job parameters are visible in history")
    void testVerifyJobParametersInHistory() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToHistory();
        searchForJob("Test Batch Job - External Trigger");

        // The history table renders execution.parameters (numberOfChunks, chunkSize, processScenario)
        // extracted from the ExampleBatchJobRequest via JobParameterExtractor
        String pageContent = page.content();
        assertTrue(
                pageContent.contains("numberOfChunks")
                        && pageContent.contains("chunkSize")
                        && pageContent.contains("processScenario"),
                "History should show the batch job parameters: numberOfChunks, chunkSize, processScenario");
    }

    @Test
    @Order(5)
    @DisplayName("Open batch job detail page from history")
    void testOpenJobDetailsPage() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToHistory();
        searchForJob("Test Batch Job - External Trigger");

        Locator jobLink = page.locator("a.text-decoration-none strong:has-text('Test Batch Job - External Trigger')")
                .first();
        jobLink.waitFor();
        jobLink.click();

        page.waitForSelector("h1:has-text('Job-Details')");
        assertTrue(page.url().contains("/q/jobrunr-control/history/details"));
        assertTrue(page.locator("h1:has-text('Job-Details')").isVisible(),
                "Job detail page should be visible");
    }
}

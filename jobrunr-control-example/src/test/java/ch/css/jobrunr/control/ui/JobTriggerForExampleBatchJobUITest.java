package ch.css.jobrunr.control.ui;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI test for creating an ExampleBatchJob with external trigger and verifying execution in history.
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

        // The history table renders execution.parameters (numberOfChunks, chunkSize, simulateErrors)
        // extracted from the ExampleBatchJobRequest via JobParameterExtractor
        String pageContent = page.content();
        assertTrue(
                pageContent.contains("numberOfChunks")
                        && pageContent.contains("chunkSize")
                        && pageContent.contains("simulateErrors"),
                "History should show the batch job parameters: numberOfChunks, chunkSize, simulateErrors");
    }
}

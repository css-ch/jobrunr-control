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
public class JobTriggerForExampleBatchJobUITest extends JobTriggerUITestBase {

    @Test
    @Order(1)
    @DisplayName("Create a batch job with external trigger via UI")
    public void testCreateJobWithExternalTrigger() {
        navigateToScheduledJobsPage();
        openJobCreationDialog();
        selectJobType("ExampleBatchJob");
        fillJobName("Test Batch Job - External Trigger");
        fillBatchJobParametersWithDefaults();
        enableExternalTrigger();
        submitJobCreationForm();

        scheduledJobId = extractJobIdFromScheduledJobsTable("Test Batch Job - External Trigger");
        System.out.println("Created scheduled batch job ID: " + scheduledJobId);
    }

    @Test
    @Order(2)
    @DisplayName("Trigger the batch job via REST API")
    public void testTriggerJobViaRest() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        String response = triggerJobViaApi(scheduledJobId);
        System.out.println("Trigger response: " + response);
        assertTrue(response.contains("Job started successfully"), "Job should be started successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Check batch job execution in history")
    public void testCheckJobExecutionInHistory() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToHistory();
        searchForJob("Test Batch Job - External Trigger");
        verifyJobInHistory("Test Batch Job - External Trigger", "Example Batch Job");
    }

    @Test
    @Order(4)
    @DisplayName("Verify job exists in JobRunr Pro Dashboard with correct signature")
    public void testVerifyJobInJobRunrDashboard() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToJobRunrDashboard(scheduledJobId);

        // The actual parameters used in the test are: numberOfChunks=10, chunkSize=50, simulateErrors=false
        // But we need to check for the signature that includes the parameters
        String expectedJobSignature = "ExampleBatchJob.run({\"numberOfChunks\":10,\"chunkSize\":50,\"simulateErrors\":false});";
        verifyJobSignatureInDashboard(expectedJobSignature);
    }
}

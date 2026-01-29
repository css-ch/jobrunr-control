package ch.css.jobrunr.control.ui;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UI test for creating a ParameterDemoJob with external trigger and verifying execution in history.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JobTriggerForParameterDemoJobUITest extends JobTriggerUITestBase {

    @Test
    @Order(1)
    @DisplayName("Create a job with external trigger via UI")
    public void testCreateJobWithExternalTrigger() {
        navigateToScheduledJobsPage();
        openJobCreationDialog();
        selectJobType("ParameterDemoJob");
        fillJobName("Test Job - External Trigger");
        fillParameterDemoJobParametersWithDefaults();
        enableExternalTrigger();
        submitJobCreationForm();

        scheduledJobId = extractJobIdFromScheduledJobsTable("Test Job - External Trigger");
        System.out.println("Created scheduled job ID: " + scheduledJobId);
    }

    @Test
    @Order(2)
    @DisplayName("Trigger the job via REST API")
    public void testTriggerJobViaRest() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        String response = triggerJobViaApi(scheduledJobId);
        System.out.println("Trigger response: " + response);
        assertTrue(response.contains("Job started successfully"), "Job should be started successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Check job execution in history")
    public void testCheckJobExecutionInHistory() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToHistory();
        searchForJob("Test Job - External Trigger");
        verifyJobInHistory("Test Job - External Trigger", "Parameter Demo Job");
    }
}

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
class JobTriggerForParameterDemoJobUITest extends JobTriggerUITestBase {

    @Test
    @Order(1)
    @DisplayName("Create a job with external trigger via UI")
    void testCreateJobWithExternalTrigger() {
        navigateToScheduledJobsPage();
        openJobCreationDialog();
        selectJobType("ParameterDemoJob");
        fillJobName("Test Job - External Trigger");
        fillParameterDemoJobParametersWithDefaults();
        enableExternalTrigger();
        submitJobCreationForm();

        scheduledJobId = extractJobIdFromScheduledJobsTable("Test Job - External Trigger");
        assertNotNull(scheduledJobId, "Scheduled job should be created and its ID extracted from the table");

    }

    @Test
    @Order(2)
    @DisplayName("Trigger the job via REST API")
    void testTriggerJobViaRest() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        String response = triggerJobViaApi(scheduledJobId);
        assertTrue(response.contains("Job started successfully"), "Job should be started successfully");
    }

    @Test
    @Order(3)
    @DisplayName("Check job execution in history")
    void testCheckJobExecutionInHistory() {
        assertNotNull(scheduledJobId, "Job ID should be set from previous test");

        navigateToHistory();
        searchForJob("Test Job - External Trigger");
        verifyJobInHistory("Test Job - External Trigger", "Parameter Demo Job");
    }
}

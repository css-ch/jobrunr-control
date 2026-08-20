package ch.css.jobrunr.control.ui;

import com.microsoft.playwright.Locator;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
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

        String requestBody = """
                {
                    "postfix": "-testrun",
                    "parameters": {
                        "stringParameter": "yes"
                    }
                }
                """;

        String response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post(baseUrl + "api/q/jobrunr-control/api/jobs/" + scheduledJobId + "/start")
                .then()
                .statusCode(200)
                .extract()
                .asString();

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

        Locator jobLink = page.locator("a.text-decoration-none strong:has-text('Test Job - External Trigger')").first();
        jobLink.click();
        page.locator("#batch-parameters table").waitFor();

        String parameterContent = page.locator("#batch-parameters").innerText();
        assertTrue(parameterContent.contains("stringParameter") && parameterContent.contains("Test String Value"),
                "Job detail should show the configured parameter 'stringParameter' with its value");
    }
}

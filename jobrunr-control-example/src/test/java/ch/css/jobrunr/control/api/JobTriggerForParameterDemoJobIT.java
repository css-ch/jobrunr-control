package ch.css.jobrunr.control.api;

import ch.css.jobrunr.control.application.scheduling.CreateScheduledJobUseCase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the create → trigger → verify flow of ParameterDemoJob.
 * Uses real Quarkus context with H2 database and JobRunr background job server.
 * Does not use Playwright — interaction via use cases and REST API only.
 */
@QuarkusTest
@DisplayName("ParameterDemoJob: create, trigger and verify via REST")
class JobTriggerForParameterDemoJobIT {

    private static final String JOB_TYPE = "ParameterDemoJob";
    private static final String JOB_NAME = "IT Test Job - External Trigger";
    private static final String API_BASE = "/q/jobrunr-control/api/jobs/";
    private static final int STATUS_POLL_TIMEOUT_SECONDS = 30;

    @Inject
    CreateScheduledJobUseCase createScheduledJobUseCase;

    @Test
    @DisplayName("should create job with external trigger, start it via REST and reach SUCCEEDED status")
    void createTriggerAndVerifyJob() throws InterruptedException {
        // --- Step 1: Create job with external trigger via use case ---
        Map<String, String> parameters = Map.of(
                "stringParameter", "Integration Test Value",
                "multilineParameter", "Line 1\nLine 2",
                "integerParameter", "42",
                "doubleParameter", "3.14159",
                "booleanParameter", "true",
                "dateParameter", "2024-01-01",
                "dateTimeParameter", "2024-01-01T12:00:00",
                "enumParameter", "OPTION_B",
                "multiEnumParameter", "OPTION_A,OPTION_C"
        );

        UUID jobId = createScheduledJobUseCase.execute(JOB_TYPE, JOB_NAME, parameters, null, true);
        assertNotNull(jobId);

        // --- Step 2: Trigger job via REST API with parameter override ---
        String startBody = """
                {
                    "postfix": "-itrun",
                    "parameters": {
                        "test-parameter": "yes"
                    }
                }
                """;

        String startResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(startBody)
                .when()
                .post(API_BASE + jobId + "/start")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(startResponse.contains("Job started successfully"), "Start response: " + startResponse);

        // --- Step 3: Poll status until SUCCEEDED (or fail after timeout) ---
        String finalStatus = pollUntilTerminal(jobId);
        assertEquals("SUCCEEDED", finalStatus,
                "Job should reach SUCCEEDED status, but was: " + finalStatus);
    }

    private String pollUntilTerminal(UUID jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STATUS_POLL_TIMEOUT_SECONDS * 1000L;
        while (System.currentTimeMillis() < deadline) {
            String statusResponse = RestAssured.given()
                    .when()
                    .get(API_BASE + jobId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            if (statusResponse.contains("SUCCEEDED") || statusResponse.contains("FAILED")) {
                if (statusResponse.contains("SUCCEEDED")) return "SUCCEEDED";
                if (statusResponse.contains("FAILED")) return "FAILED";
            }
            Thread.sleep(1000);
        }
        return "TIMEOUT";
    }
}

package ch.css.jobrunr.control.api;

import ch.css.jobrunr.control.application.template.CreateTemplateUseCase;
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
 * Integration test for the create template → trigger by name → verify flow of ParameterDemoJob.
 * Uses real Quarkus context with H2 database and JobRunr background job server.
 * Does not use Playwright — interaction via use cases and REST API only.
 */
@QuarkusTest
@DisplayName("ParameterDemoJob: create template, trigger by name via REST and verify")
class JobTriggerByTemplateNameIT {

    private static final String JOB_TYPE = "ParameterDemoJob";
    private static final String TEMPLATE_NAME = "IT Template - Trigger By Name";
    private static final String API_BASE = "/q/jobrunr-control/api/jobs/";
    private static final int STATUS_POLL_TIMEOUT_SECONDS = 30;

    @Inject
    CreateTemplateUseCase createTemplateUseCase;

    @Test
    @DisplayName("should create template, start it via REST using template name and reach SUCCEEDED status")
    void createTemplateTriggerByNameAndVerify() throws InterruptedException {
        // --- Step 1: Create template via use case ---
        Map<String, String> parameters = Map.of(
                "stringParameter", "Template Name Trigger Test",
                "multilineParameter", "Line 1\nLine 2",
                "integerParameter", "7",
                "doubleParameter", "2.71828",
                "booleanParameter", "false",
                "dateParameter", "2025-06-01",
                "dateTimeParameter", "2025-06-01T08:00:00",
                "enumParameter", "OPTION_A",
                "multiEnumParameter", "OPTION_B"
        );

        UUID templateId = createTemplateUseCase.execute(JOB_TYPE, TEMPLATE_NAME, parameters);
        assertNotNull(templateId);

        // --- Step 2: Trigger via REST API using template name (not UUID) ---
        String startBody = """
                {
                    "postfix": "-itrun"
                }
                """;

        String startResponse = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(startBody)
                .when()
                .post(API_BASE + TEMPLATE_NAME + "/start")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        assertTrue(startResponse.contains("Template job started successfully"),
                "Start response should indicate template was started, but was: " + startResponse);

        // Extract the result job ID from the response
        UUID resultJobId = extractJobId(startResponse);
        assertNotNull(resultJobId, "Result job ID must be present in response: " + startResponse);
        assertNotEquals(templateId, resultJobId, "Result job ID must differ from template ID (clone was created)");

        // --- Step 3: Poll status until SUCCEEDED (or fail after timeout) ---
        String finalStatus = pollUntilTerminal(resultJobId);
        assertEquals("SUCCEEDED", finalStatus,
                "Job should reach SUCCEEDED status, but was: " + finalStatus);
    }

    private UUID extractJobId(String jsonResponse) {
        // Parse "jobId":"<uuid>" from JSON response
        int idx = jsonResponse.indexOf("\"jobId\"");
        if (idx < 0) return null;
        int start = jsonResponse.indexOf("\"", idx + 7) + 1;
        int end = jsonResponse.indexOf("\"", start);
        if (start <= 0 || end <= start) return null;
        try {
            return UUID.fromString(jsonResponse.substring(start, end));
        } catch (IllegalArgumentException e) {
            return null;
        }
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

            if (statusResponse.contains("SUCCEEDED")) return "SUCCEEDED";
            if (statusResponse.contains("FAILED")) return "FAILED";
            Thread.sleep(1000);
        }
        return "TIMEOUT";
    }
}

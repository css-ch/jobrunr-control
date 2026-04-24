package ch.css.jobrunr.control.api;

import ch.css.jobrunr.control.application.scheduling.CreateScheduledJobUseCase;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
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
 * Integration test for the jobType override feature. The handler
 * {@code LegacyCustomerDataArchivalAndCleanupJobHandler} has a 46-character simple class name
 * and relies on {@code @ConfigurableJob(jobType = "LegacyArchival")} so the
 * {@code jobtype:LegacyArchival} label fits within JobRunr's 45-character budget.
 * <p>
 * The test proves the override is honored end-to-end: discovery registers the override,
 * scheduling succeeds (which would throw from JobRunr if the label were too long), and the
 * job executes to completion.
 */
@QuarkusTest
@DisplayName("LegacyArchival (jobType override): create, trigger and verify via REST")
class JobTriggerForLegacyArchivalJobIT {

    private static final String JOB_TYPE = "LegacyArchival";
    private static final String HANDLER_CLASS_NAME = "LegacyCustomerDataArchivalAndCleanupJobHandler";
    private static final String JOB_NAME = "IT Legacy Archival - External Trigger";
    private static final String API_BASE = "/api/q/jobrunr-control/api/jobs/";
    private static final int STATUS_POLL_TIMEOUT_SECONDS = 30;

    @Inject
    CreateScheduledJobUseCase createScheduledJobUseCase;

    @Inject
    JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Test
    @DisplayName("jobType override is discovered and routes back to the long-named handler")
    void discoveryResolvesShortJobTypeToLongHandler() {
        JobDefinition definition = jobDefinitionDiscoveryService.findJobByType(JOB_TYPE)
                .orElseThrow(() -> new AssertionError(
                        "Expected to find a JobDefinition with jobType '" + JOB_TYPE + "' — "
                                + "the @ConfigurableJob(jobType = ...) override is not being applied."));

        assertEquals(JOB_TYPE, definition.jobType(),
                "Discovered jobType must match the annotation override, not the handler simple name");
        assertTrue(definition.handlerClassName().endsWith("." + HANDLER_CLASS_NAME),
                "JobDefinition must still point at the long-named handler class; was: "
                        + definition.handlerClassName());
    }

    @Test
    @DisplayName("should create job via override, start it via REST and reach SUCCEEDED status")
    void createTriggerAndVerifyJob() throws InterruptedException {
        Map<String, String> parameters = Map.of(
                "archiveBefore", "2020-01-01",
                "dryRun", "true"
        );

        UUID jobId = createScheduledJobUseCase.execute(JOB_TYPE, JOB_NAME, parameters, null, true);
        assertNotNull(jobId, "Scheduling must succeed — failure here would indicate the "
                + "'jobtype:' label exceeded JobRunr's 45-char limit.");

        String startBody = """
                {
                    "postfix": "-itrun",
                    "parameters": {}
                }
                """;

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(startBody)
                .when()
                .post(API_BASE + jobId + "/start")
                .then()
                .statusCode(200);

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

            if (statusResponse.contains("SUCCEEDED")) return "SUCCEEDED";
            if (statusResponse.contains("FAILED")) return "FAILED";
            Thread.sleep(1000);
        }
        return "TIMEOUT";
    }
}

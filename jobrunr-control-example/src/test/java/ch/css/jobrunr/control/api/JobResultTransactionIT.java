package ch.css.jobrunr.control.api;

import ch.css.jobrunr.control.application.scheduling.CreateScheduledJobUseCase;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobResultAdapter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import org.jobrunr.storage.StorageProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that a result written by JobResultAdapter survives the rollback of
 * the transaction surrounding the post-processing job.
 */
@QuarkusTest
@DisplayName("JobResultAdapter: result survives rollback")
class JobResultTransactionIT {

    private static final String API_BASE = "/api/q/jobrunr-control/api/jobs/";
    private static final int STATUS_POLL_TIMEOUT_SECONDS = 90;

    @Inject
    CreateScheduledJobUseCase createScheduledJobUseCase;

    @Inject
    StorageProvider storageProvider;

    @Test
    @DisplayName("stores result code 50 when post-processing transaction rolls back")
    void resultSurvivesPostProcessingRollback() throws InterruptedException {
        UUID jobId = createScheduledJobUseCase.execute(
                "ExampleBatchJob",
                "IT Result Transaction Rollback",
                Map.of(
                        "parameters.numberOfChunks", "1",
                        "parameters.chunkSize", "1",
                        "parameters.processScenario", "POSTJOB_ERROR"
                ),
                null,
                true
        );
        assertNotNull(jobId);

        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"postfix\":\"-itrun\",\"parameters\":{}}")
                .when()
                .post(API_BASE + jobId + "/start")
                .then()
                .statusCode(200);

        assertEquals(50, pollResultCode(jobId));
    }

    private int pollResultCode(UUID jobId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + STATUS_POLL_TIMEOUT_SECONDS * 1000L;
        Object lastResultCode = null;

        while (System.currentTimeMillis() < deadline) {
            lastResultCode = storageProvider.getJobById(jobId)
                    .getMetadata()
                    .get(JobResultAdapter.RESULT_CODE_METADATA_KEY);
            if (lastResultCode != null) {
                return ((Number) lastResultCode).intValue();
            }
            Thread.sleep(500);
        }

        throw new AssertionError("Job result was not persisted within "
                + STATUS_POLL_TIMEOUT_SECONDS + " seconds; last result code: " + lastResultCode);
    }
}

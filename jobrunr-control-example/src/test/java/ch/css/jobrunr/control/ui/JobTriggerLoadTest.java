package ch.css.jobrunr.control.ui;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load test for creating and starting 1000 batch jobs with 1000 items each.
 * This test validates the system's ability to handle high-volume job creation and execution.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class JobTriggerLoadTest extends JobTriggerUITestBase {

    private static final int NUMBER_OF_JOBS = 1000;
    private static final int ITEMS_PER_JOB = 10000;
    private static final int CHUNK_SIZE = 5;

    private final List<UUID> createdJobIds = new ArrayList<>();

    @Test
    @Order(1)
    @DisplayName("Create 1000 batch jobs via UI")
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void testCreateBatchJobs() {
        navigateToScheduledJobsPage();

        long startTime = System.currentTimeMillis();
        System.out.println("Starting creation of " + NUMBER_OF_JOBS + " batch jobs...");

        for (int i = 1; i <= NUMBER_OF_JOBS; i++) {
            String jobName = String.format("Load Test Batch Job %04d", i);

            try {
                openJobCreationDialog();
                selectJobType("ExampleBatchJob");
                fillJobName(jobName);
                fillBatchJobParametersForLoadTest();
                enableExternalTrigger();
                submitJobCreationForm();

                UUID jobId = extractJobIdFromScheduledJobsTable(jobName);
                createdJobIds.add(jobId);

                if (i % 100 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double jobsPerSecond = (i * 1000.0) / elapsed;
                    System.out.printf("Created %d/%d jobs (%.2f jobs/sec)%n",
                            i, NUMBER_OF_JOBS, jobsPerSecond);
                }

            } catch (Exception e) {
                System.err.printf("Failed to create job %d (%s): %s%n", i, jobName, e.getMessage());
                // Continue with next job
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgJobsPerSecond = (createdJobIds.size() * 1000.0) / totalTime;

        System.out.printf("%nJob Creation Summary:%n");
        System.out.printf("  Successfully created: %d/%d jobs%n", createdJobIds.size(), NUMBER_OF_JOBS);
        System.out.printf("  Total time: %.2f seconds%n", totalTime / 1000.0);
        System.out.printf("  Average: %.2f jobs/second%n", avgJobsPerSecond);

        assertFalse(createdJobIds.isEmpty(), "At least some jobs should be created");
        assertEquals(NUMBER_OF_JOBS, createdJobIds.size(),
                "All jobs should be created successfully");
    }

    @Test
    @Order(2)
    @DisplayName("Start all 1000 batch jobs via REST API")
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void testStartAllBatchJobs() {
        assertFalse(createdJobIds.isEmpty(), "Jobs must be created before starting");

        long startTime = System.currentTimeMillis();
        System.out.println("Starting " + createdJobIds.size() + " batch jobs...");

        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < createdJobIds.size(); i++) {
            UUID jobId = createdJobIds.get(i);

            try {
                String response = triggerJobViaApi(jobId);

                if (response.contains("Job started successfully")) {
                    successCount++;
                } else {
                    failureCount++;
                    System.err.printf("Unexpected response for job %s: %s%n", jobId, response);
                }

                if ((i + 1) % 100 == 0) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    double startsPerSecond = ((i + 1) * 1000.0) / elapsed;
                    System.out.printf("Started %d/%d jobs (%.2f starts/sec, %d failed)%n",
                            i + 1, createdJobIds.size(), startsPerSecond, failureCount);
                }

            } catch (Exception e) {
                failureCount++;
                System.err.printf("Failed to start job %s: %s%n", jobId, e.getMessage());
                // Continue with next job
            }
        }

        long totalTime = System.currentTimeMillis() - startTime;
        double avgStartsPerSecond = (successCount * 1000.0) / totalTime;

        System.out.printf("%nJob Start Summary:%n");
        System.out.printf("  Successfully started: %d/%d jobs%n", successCount, createdJobIds.size());
        System.out.printf("  Failed to start: %d jobs%n", failureCount);
        System.out.printf("  Total time: %.2f seconds%n", totalTime / 1000.0);
        System.out.printf("  Average: %.2f starts/second%n", avgStartsPerSecond);
        System.out.printf("  Total items to process: %,d (%d jobs × %d items)%n",
                successCount * ITEMS_PER_JOB, successCount, ITEMS_PER_JOB);

        assertTrue(successCount > 0, "At least some jobs should be started");
        assertEquals(createdJobIds.size(), successCount,
                "All created jobs should start successfully");
    }

    @AfterAll
    public void printFinalSummary() {
        System.out.printf("%n=== Load Test Final Summary ===%n");
        System.out.printf("Jobs created: %d%n", createdJobIds.size());
        System.out.printf("Items per job: %d%n", ITEMS_PER_JOB);
        System.out.printf("Chunk size: %d%n", CHUNK_SIZE);
        System.out.printf("Total items queued for processing: %,d%n",
                createdJobIds.size() * ITEMS_PER_JOB);
        System.out.printf("Expected child jobs: %,d%n",
                createdJobIds.size() * (ITEMS_PER_JOB / CHUNK_SIZE));
    }

    private void fillBatchJobParametersForLoadTest() {
        // Calculate number of chunks: 1000 items / 100 per chunk = 10 chunks
        int numberOfChunks = ITEMS_PER_JOB / CHUNK_SIZE;

        page.fill("input[name='parameters.numberOfChunks']", String.valueOf(numberOfChunks));
        page.fill("input[name='parameters.chunkSize']", String.valueOf(CHUNK_SIZE));
        page.selectOption("select[name='parameters.simulateErrors']", "false");
    }
}

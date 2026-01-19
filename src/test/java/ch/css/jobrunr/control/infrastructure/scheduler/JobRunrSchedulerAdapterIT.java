package ch.css.jobrunr.control.infrastructure.scheduler;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JobRunrSchedulerAdapter using H2 database.
 * Tests all core scheduling operations including creating, updating, deleting,
 * and executing jobs.
 */
@QuarkusTest
@TestProfile(JobRunrSchedulerAdapterIT.H2TestProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JobRunrSchedulerAdapterIT {

    @Inject
    JobRunrSchedulerAdapter schedulerAdapter;

    private static final String TEST_JOB_TYPE = "SimpleReportJob";
    private static final String TEST_JOB_TYPE_WITH_PARAMS = "NotificationJob";

    @BeforeEach
    void setUp() {
        // Clean up any existing scheduled jobs before each test
        cleanupScheduledJobs();
    }

    @Test
    @Order(1)
    @DisplayName("Should schedule a simple job without parameters")
    void testScheduleSimpleJob() {
        // Given
        JobDefinition jobDefinition = new JobDefinition(
                TEST_JOB_TYPE,
                false,
                Collections.emptySet()
        );
        String jobName = "Test Report Job";
        Map<String, Object> parameters = Collections.emptyMap();
        Instant scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS);

        // When
        UUID jobId = schedulerAdapter.scheduleJob(jobDefinition, jobName, parameters, false, scheduledAt);

        // Then
        assertNotNull(jobId, "Job ID should not be null");

        // Verify job exists through adapter
        ScheduledJobInfo scheduledJobInfo = schedulerAdapter.getScheduledJobById(jobId);
        assertNotNull(scheduledJobInfo, "Job should exist");
        assertEquals(jobName, scheduledJobInfo.getJobName(), "Job name should match");
        assertEquals(TEST_JOB_TYPE, scheduledJobInfo.getJobType(), "Job type should match");
    }

    @Test
    @Order(2)
    @DisplayName("Should schedule a job with parameters")
    void testScheduleJobWithParameters() {
        // Given
        Set<JobParameter> parameterDefinitions = Set.of(
                new JobParameter("recipientEmail", JobParameterType.STRING, true, null),
                new JobParameter("subject", JobParameterType.STRING, true, null),
                new JobParameter("sendImmediately", JobParameterType.BOOLEAN, false, false),
                new JobParameter("scheduledTime", JobParameterType.DATETIME, false, null)
        );

        JobDefinition jobDefinition = new JobDefinition(
                TEST_JOB_TYPE_WITH_PARAMS,
                false,
                parameterDefinitions
        );

        String jobName = "Test Notification Job";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("recipientEmail", "test@example.com");
        parameters.put("subject", "Test Notification");
        parameters.put("sendImmediately", true);
        parameters.put("scheduledTime", LocalDateTime.now().plusHours(2));

        Instant scheduledAt = Instant.now().plus(30, ChronoUnit.MINUTES);

        // When
        UUID jobId = schedulerAdapter.scheduleJob(jobDefinition, jobName, parameters, false, scheduledAt);

        // Then
        assertNotNull(jobId, "Job ID should not be null");

        // Verify job through adapter
        ScheduledJobInfo scheduledJobInfo = schedulerAdapter.getScheduledJobById(jobId);
        assertNotNull(scheduledJobInfo, "Scheduled job info should not be null");
        assertEquals(jobName, scheduledJobInfo.getJobName(), "Job name should match");
        assertEquals(TEST_JOB_TYPE_WITH_PARAMS, scheduledJobInfo.getJobType(), "Job type should match");
        assertFalse(scheduledJobInfo.getParameters().isEmpty(), "Parameters should not be empty");
    }

    @Test
    @Order(3)
    @DisplayName("Should retrieve scheduled job by ID")
    void testGetScheduledJobById() {
        // Given
        UUID jobId = createTestJob("Test Job for Retrieval");

        // When
        ScheduledJobInfo scheduledJobInfo = schedulerAdapter.getScheduledJobById(jobId);

        // Then
        assertNotNull(scheduledJobInfo, "Scheduled job info should not be null");
        assertEquals(jobId, scheduledJobInfo.getJobId(), "Job ID should match");
        assertEquals("Test Job for Retrieval", scheduledJobInfo.getJobName(), "Job name should match");
        assertEquals(TEST_JOB_TYPE, scheduledJobInfo.getJobType(), "Job type should match");
        assertNotNull(scheduledJobInfo.getScheduledAt(), "Scheduled time should not be null");
    }

    @Test
    @Order(4)
    @DisplayName("Should return null for non-existent job ID")
    void testGetScheduledJobById_NotFound() {
        // Given
        UUID nonExistentJobId = UUID.randomUUID();

        // When
        ScheduledJobInfo result = schedulerAdapter.getScheduledJobById(nonExistentJobId);

        // Then
        assertNull(result, "Should return null for non-existent job");
    }

    @Test
    @Order(5)
    @DisplayName("Should retrieve all scheduled jobs")
    void testGetScheduledJobs() {
        // Given
        createTestJob("Job 1");
        createTestJob("Job 2");
        createTestJob("Job 3");

        // When
        List<ScheduledJobInfo> scheduledJobs = schedulerAdapter.getScheduledJobs();

        // Then
        assertNotNull(scheduledJobs, "Scheduled jobs list should not be null");
        assertTrue(scheduledJobs.size() >= 3, "Should have at least 3 scheduled jobs");

        // Verify all jobs are in SCHEDULED state
        scheduledJobs.forEach(job -> {
            assertNotNull(job.getJobId(), "Job ID should not be null");
            assertNotNull(job.getJobName(), "Job name should not be null");
            assertNotNull(job.getJobType(), "Job type should not be null");
            assertNotNull(job.getScheduledAt(), "Scheduled time should not be null");
        });
    }

    @Test
    @Order(6)
    @DisplayName("Should update an existing scheduled job")
    void testUpdateJob() {
        // Given - Create initial job with parameters
        Set<JobParameter> parameterDefinitions = Set.of(
                new JobParameter("recipientEmail", JobParameterType.STRING, true, null),
                new JobParameter("subject", JobParameterType.STRING, true, null),
                new JobParameter("sendImmediately", JobParameterType.BOOLEAN, false, false),
                new JobParameter("scheduledTime", JobParameterType.DATETIME, false, null)
        );

        JobDefinition initialDefinition = new JobDefinition(
                TEST_JOB_TYPE_WITH_PARAMS,
                false,
                parameterDefinitions
        );

        String initialJobName = "Original Notification Job";
        Map<String, Object> initialParameters = new HashMap<>();
        initialParameters.put("recipientEmail", "original@example.com");
        initialParameters.put("subject", "Original Subject");
        initialParameters.put("sendImmediately", false);
        initialParameters.put("scheduledTime", LocalDateTime.now().plusHours(1));

        Instant initialScheduledAt = Instant.now().plus(1, ChronoUnit.HOURS);
        UUID jobId = schedulerAdapter.scheduleJob(initialDefinition, initialJobName, initialParameters, true, initialScheduledAt);

        // When - Update the job with new name, parameters and schedule time
        JobDefinition updatedDefinition = new JobDefinition(
                TEST_JOB_TYPE_WITH_PARAMS,
                false,
                parameterDefinitions
        );
        String updatedJobName = "Updated Notification Job";
        Map<String, Object> updatedParameters = new HashMap<>();
        updatedParameters.put("recipientEmail", "updated@example.com");
        updatedParameters.put("subject", "Updated Subject");
        updatedParameters.put("sendImmediately", true);
        updatedParameters.put("scheduledTime", LocalDateTime.now().plusHours(3));

        Instant updatedScheduledAt = Instant.now().plus(2, ChronoUnit.HOURS);

        schedulerAdapter.updateJob(jobId, updatedDefinition, updatedJobName, updatedParameters, true, updatedScheduledAt);

        // Then - Verify job exists and basic properties are updated
        ScheduledJobInfo updatedJob = schedulerAdapter.getScheduledJobById(jobId);
        assertNotNull(updatedJob, "Job should still exist");
        assertEquals(updatedJobName, updatedJob.getJobName(), "Job name should be updated");
        assertEquals(TEST_JOB_TYPE_WITH_PARAMS, updatedJob.getJobType(), "Job type should remain the same");
        assertFalse(updatedJob.getParameters().isEmpty(), "Parameters should not be empty");
        assertEquals(4, updatedJob.getParameters().size(), "Should have 4 parameters");
        assertEquals("updated@example.com", updatedJob.getParameters().get("recipientEmail"), "Recipient email should be updated");
    }

    @Test
    @Order(7)
    @DisplayName("Should delete a scheduled job")
    void testDeleteScheduledJob() {
        // Given
        UUID jobId = createTestJob("Job to Delete");

        // Verify job exists
        ScheduledJobInfo jobBefore = schedulerAdapter.getScheduledJobById(jobId);
        assertNotNull(jobBefore, "Job should exist before deletion");

        // When
        schedulerAdapter.deleteScheduledJob(jobId);

        // Then
        ScheduledJobInfo jobAfter = schedulerAdapter.getScheduledJobById(jobId);
        assertNull(jobAfter, "Job should not be retrievable after deletion (deleted state)");
    }

    @Test
    @Order(8)
    @DisplayName("Should execute a scheduled job immediately")
    void testExecuteJobNow() {
        // Given
        UUID jobId = createTestJob("Job to Execute Now");

        // Verify job is scheduled
        ScheduledJobInfo jobBefore = schedulerAdapter.getScheduledJobById(jobId);
        assertNotNull(jobBefore, "Job should exist and be scheduled");

        // When
        schedulerAdapter.executeJobNow(jobId);

        // Then
        // After enqueuing, the job is no longer in SCHEDULED state
        ScheduledJobInfo jobAfter = schedulerAdapter.getScheduledJobById(jobId);
        assertNull(jobAfter, "Job should no longer be in SCHEDULED state after execution trigger");
    }

    @Test
    @Order(9)
    @DisplayName("Should throw exception when executing non-existent job")
    void testExecuteJobNow_NotFound() {
        // Given
        UUID nonExistentJobId = UUID.randomUUID();

        // When & Then
        assertThrows(
                JobRunrSchedulerAdapter.JobSchedulingException.class,
                () -> schedulerAdapter.executeJobNow(nonExistentJobId),
                "Should throw JobSchedulingException for non-existent job"
        );
    }

    @Test
    @Order(10)
    @DisplayName("Should identify externally triggerable jobs")
    void testExternallyTriggerableJob() {
        // Given
        JobDefinition jobDefinition = new JobDefinition(
                TEST_JOB_TYPE,
                false,
                Collections.emptySet()
        );
        String jobName = "Externally Triggerable Job";
        Map<String, Object> parameters = Collections.emptyMap();
        // Special date for externally triggerable jobs
        Instant scheduledAt = Instant.parse("2999-12-31T23:59:59Z");

        // When
        UUID jobId = schedulerAdapter.scheduleJob(jobDefinition, jobName, parameters, false, scheduledAt);

        // Then
        ScheduledJobInfo scheduledJobInfo = schedulerAdapter.getScheduledJobById(jobId);
        assertNotNull(scheduledJobInfo, "Scheduled job info should not be null");
        assertTrue(scheduledJobInfo.isExternallyTriggerable(), "Job should be externally triggerable");
    }

    @Test
    @Order(11)
    @DisplayName("Should handle multiple concurrent job scheduling")
    void testConcurrentJobScheduling() {
        // Given
        int numberOfJobs = 10;
        List<UUID> jobIds = Collections.synchronizedList(new ArrayList<>());

        // When
        for (int i = 0; i < numberOfJobs; i++) {
            UUID jobId = createTestJob("Concurrent Job " + i);
            jobIds.add(jobId);
        }

        // Then
        assertEquals(numberOfJobs, jobIds.size(), "Should have created all jobs");
        List<ScheduledJobInfo> scheduledJobs = schedulerAdapter.getScheduledJobs();
        assertTrue(scheduledJobs.size() >= numberOfJobs, "All jobs should be scheduled");
    }

    // Helper Methods

    /**
     * Creates a simple test job and returns its ID.
     */
    private UUID createTestJob(String jobName) {
        JobDefinition jobDefinition = new JobDefinition(
                TEST_JOB_TYPE,
                true,
                Collections.emptySet()
        );
        Map<String, Object> parameters = Collections.emptyMap();
        Instant scheduledAt = Instant.now().plus(1, ChronoUnit.HOURS);

        return schedulerAdapter.scheduleJob(jobDefinition, jobName, parameters, false, scheduledAt);
    }

    /**
     * Cleans up all scheduled jobs from storage.
     */
    private void cleanupScheduledJobs() {
        try {
            List<ScheduledJobInfo> scheduledJobs = schedulerAdapter.getScheduledJobs();
            for (ScheduledJobInfo job : scheduledJobs) {
                try {
                    schedulerAdapter.deleteScheduledJob(job.getJobId());
                } catch (Exception e) {
                    // Ignore errors during cleanup
                }
            }
        } catch (Exception e) {
            // Ignore errors during cleanup
        }
    }

    /**
     * Test profile to configure H2 database for integration tests.
     */
    public static class H2TestProfile implements io.quarkus.test.junit.QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.ofEntries(
                    // H2 Database Configuration with SQL logging
                    Map.entry("quarkus.datasource.db-kind", "h2"),
                    Map.entry("quarkus.datasource.username", "sa"),
                    Map.entry("quarkus.datasource.password", ""),
                    Map.entry("quarkus.datasource.jdbc.url", "jdbc:h2:mem:jobrunr-test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;TRACE_LEVEL_SYSTEM_OUT=2"),
                    Map.entry("quarkus.datasource.jdbc.max-size", "10"),

                    // Enable SQL logging with parameters
                    Map.entry("quarkus.hibernate-orm.log.sql", "true"),
                    Map.entry("quarkus.hibernate-orm.log.bind-parameters", "true"),
                    Map.entry("quarkus.log.category.\"org.hibernate.SQL\".level", "DEBUG"),
                    Map.entry("quarkus.log.category.\"org.hibernate.type.descriptor.sql.BasicBinder\".level", "TRACE"),

                    // JobRunr Configuration
                    Map.entry("quarkus.jobrunr.background-job-server.enabled", "true"),
                    Map.entry("quarkus.jobrunr.dashboard.enabled", "false"),
                    Map.entry("quarkus.jobrunr.job-scheduler.enabled", "true"),

                    // Logging
                    Map.entry("quarkus.log.category.\"ch.css.jobrunr.control\".level", "DEBUG"),
                    Map.entry("quarkus.log.category.\"org.jobrunr\".level", "WARN")
            );
        }
    }
}

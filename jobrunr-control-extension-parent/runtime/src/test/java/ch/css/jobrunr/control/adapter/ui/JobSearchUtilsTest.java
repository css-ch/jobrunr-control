package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSearchUtilsTest {

    private static final JobDefinition TEST_JOB_DEFINITION = new JobDefinition(
            "TestJobType",
            false,
            "test.JobRequest",
            "test.JobHandler",
            List.of(),
            new JobSettings(null, false, 0, List.of(), List.of(), null, null, null, null, null, null, null),
            false,
            null
    );

    @Test
    void shouldReturnAllExecutionsWhenSearchIsBlank() {
        List<JobExecutionInfo> executions = createTestExecutions();

        List<JobExecutionInfo> result = JobSearchUtils.applySearchToExecutions("", executions);

        assertEquals(executions.size(), result.size());
    }

    @Test
    void shouldReturnAllExecutionsWhenSearchIsNull() {
        List<JobExecutionInfo> executions = createTestExecutions();

        List<JobExecutionInfo> result = JobSearchUtils.applySearchToExecutions(null, executions);

        assertEquals(executions.size(), result.size());
    }

    @Test
    void shouldFilterExecutionsByJobName() {
        List<JobExecutionInfo> executions = createTestExecutions();

        List<JobExecutionInfo> result = JobSearchUtils.applySearchToExecutions("monthly", executions);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getJobName().toLowerCase().contains("monthly"));
    }

    @Test
    void shouldFilterExecutionsByJobType() {
        List<JobExecutionInfo> executions = createTestExecutions();

        List<JobExecutionInfo> result = JobSearchUtils.applySearchToExecutions("BatchJob", executions);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getJobType().contains("BatchJob"));
    }

    @Test
    void shouldFilterExecutionsByParameterKeyValue() {
        List<JobExecutionInfo> executions = createTestExecutions();

        List<JobExecutionInfo> result = JobSearchUtils.applySearchToExecutions("region=EU", executions);

        assertEquals(1, result.size());
        assertEquals("EU", result.get(0).getParameters().get("region"));
    }

    @Test
    void shouldFilterExecutionsByMetadataKeyValue() {
        List<JobExecutionInfo> executions = createTestExecutions();

        List<JobExecutionInfo> result = JobSearchUtils.applySearchToExecutions("priority=high", executions);

        assertEquals(1, result.size());
        assertEquals("high", result.get(0).getMetadata().get("priority"));
    }

    @Test
    void shouldReturnAllScheduledJobsWhenSearchIsBlank() {
        List<ScheduledJobInfo> jobs = createTestScheduledJobs();

        List<ScheduledJobInfo> result = JobSearchUtils.applySearchToScheduledJobs("", jobs);

        assertEquals(jobs.size(), result.size());
    }

    @Test
    void shouldFilterScheduledJobsByJobName() {
        List<ScheduledJobInfo> jobs = createTestScheduledJobs();

        List<ScheduledJobInfo> result = JobSearchUtils.applySearchToScheduledJobs("daily", jobs);

        assertEquals(1, result.size());
        assertTrue(result.get(0).jobName().toLowerCase().contains("daily"));
    }

    @Test
    void shouldFilterScheduledJobsByParameterKeyValue() {
        List<ScheduledJobInfo> jobs = createTestScheduledJobs();

        List<ScheduledJobInfo> result = JobSearchUtils.applySearchToScheduledJobs("env=prod", jobs);

        assertEquals(1, result.size());
        assertEquals("prod", result.get(0).parameters().get("env"));
    }

    private List<JobExecutionInfo> createTestExecutions() {
        return List.of(
                new JobExecutionInfo(
                        UUID.randomUUID(),
                        "Daily Report",
                        "ReportJob",
                        JobStatus.SUCCEEDED,
                        Instant.now(),
                        Instant.now(),
                        null,
                        Map.of("env", "prod"),
                        Map.of("priority", "high")
                ),
                new JobExecutionInfo(
                        UUID.randomUUID(),
                        "Monthly Summary",
                        "BatchJob",
                        JobStatus.SUCCEEDED,
                        Instant.now(),
                        Instant.now(),
                        null,
                        Map.of("region", "EU"),
                        Map.of("priority", "medium")
                )
        );
    }

    private List<ScheduledJobInfo> createTestScheduledJobs() {
        return List.of(
                new ScheduledJobInfo(
                        UUID.randomUUID(),
                        "Daily Backup",
                        TEST_JOB_DEFINITION,
                        Instant.now(),
                        Map.of("env", "prod"),
                        false
                ),
                new ScheduledJobInfo(
                        UUID.randomUUID(),
                        "Weekly Report",
                        TEST_JOB_DEFINITION,
                        Instant.now(),
                        Map.of("type", "summary"),
                        false
                )
        );
    }
}

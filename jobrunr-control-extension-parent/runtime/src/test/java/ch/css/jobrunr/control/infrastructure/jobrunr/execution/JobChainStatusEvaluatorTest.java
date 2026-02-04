package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.JobStatus;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for JobChainStatusEvaluator.
 * Tests the simplified job chain status evaluation logic.
 */
class JobChainStatusEvaluatorTest {

    private StorageProvider storageProvider;
    private JobStateMapper jobStateMapper;
    private JobChainStatusEvaluator evaluator;

    @BeforeEach
    void setUp() {
        storageProvider = mock(StorageProvider.class);
        jobStateMapper = mock(JobStateMapper.class);
        evaluator = new JobChainStatusEvaluator(storageProvider, jobStateMapper);
    }

    @Test
    void evaluateChainStatus_withNoContinuationJobs_returnsParentStatus() {
        // Given: Parent job succeeded, no continuation jobs
        UUID parentJobId = UUID.randomUUID();
        when(storageProvider.getJobList(any(), any())).thenReturn(List.of());

        // When
        var result = evaluator.evaluateChainStatus(parentJobId, JobStatus.SUCCEEDED);

        // Then
        assertTrue(result.isComplete());
        assertEquals(JobStatus.SUCCEEDED, result.overallStatus());
    }

    @Test
    void evaluateChainStatus_withSuccessfulLeafJob_chainingComplete() {
        // Given: Parent succeeded, continuation succeeded
        UUID parentJobId = UUID.randomUUID();
        Job leafJob = createMockJob(UUID.randomUUID());

        when(storageProvider.getJobList(any(), any()))
                .thenReturn(List.of(leafJob))  // Has one child
                .thenReturn(List.of());        // Child has no children (leaf)

        when(jobStateMapper.mapJobState(any())).thenReturn(JobStatus.SUCCEEDED);

        // When
        var result = evaluator.evaluateChainStatus(parentJobId, JobStatus.SUCCEEDED);

        // Then
        assertTrue(result.isComplete());
        assertEquals(JobStatus.SUCCEEDED, result.overallStatus());
    }

    @Test
    void evaluateChainStatus_withProcessingLeafJob_chainNotComplete() {
        // Given: Parent succeeded, continuation still processing
        UUID parentJobId = UUID.randomUUID();
        Job processingJob = createMockJob(UUID.randomUUID());

        when(storageProvider.getJobList(any(), any()))
                .thenReturn(List.of(processingJob))
                .thenReturn(List.of());

        when(jobStateMapper.mapJobState(any())).thenReturn(JobStatus.PROCESSING);

        // When
        var result = evaluator.evaluateChainStatus(parentJobId, JobStatus.SUCCEEDED);

        // Then
        assertFalse(result.isComplete());
        assertEquals(JobStatus.PROCESSING, result.overallStatus());
    }

    @Test
    void evaluateChainStatus_withFailedLeafJob_chainFailedStatus() {
        // Given: Parent succeeded, but continuation failed
        UUID parentJobId = UUID.randomUUID();
        Job failedJob = createMockJob(UUID.randomUUID());

        when(storageProvider.getJobList(any(), any()))
                .thenReturn(List.of(failedJob))
                .thenReturn(List.of());

        when(jobStateMapper.mapJobState(any())).thenReturn(JobStatus.FAILED);

        // When
        var result = evaluator.evaluateChainStatus(parentJobId, JobStatus.SUCCEEDED);

        // Then
        assertTrue(result.isComplete());
        assertEquals(JobStatus.FAILED, result.overallStatus());
    }

    @Test
    void evaluateChainStatus_withMultipleLevels_findsLeafJobs() {
        // Given: Parent -> Intermediate -> Leaf structure
        UUID parentJobId = UUID.randomUUID();
        Job intermediateJob = createMockJob(UUID.randomUUID());
        Job leafJob = createMockJob(UUID.randomUUID());

        when(storageProvider.getJobList(any(), any()))
                .thenReturn(List.of(intermediateJob))  // Parent has intermediate child
                .thenReturn(List.of(leafJob))          // Intermediate has leaf child
                .thenReturn(List.of());                // Leaf has no children

        when(jobStateMapper.mapJobState(any())).thenReturn(JobStatus.SUCCEEDED);

        // When
        var result = evaluator.evaluateChainStatus(parentJobId, JobStatus.SUCCEEDED);

        // Then
        assertTrue(result.isComplete());
        assertEquals(JobStatus.SUCCEEDED, result.overallStatus());
    }

    @Test
    void evaluateChainStatus_withException_returnsFallbackStatus() {
        // Given: Storage provider throws exception
        UUID parentJobId = UUID.randomUUID();
        when(storageProvider.getJobList(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // When
        var result = evaluator.evaluateChainStatus(parentJobId, JobStatus.SUCCEEDED);

        // Then
        assertTrue(result.isComplete());
        assertEquals(JobStatus.SUCCEEDED, result.overallStatus());
    }

    /**
     * Helper method to create a mock Job.
     */
    private Job createMockJob(UUID jobId) {
        Job job = mock(Job.class);
        JobState jobState = mock(JobState.class);

        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(jobState);
        when(job.getJobStates()).thenReturn(List.of());

        return job;
    }
}

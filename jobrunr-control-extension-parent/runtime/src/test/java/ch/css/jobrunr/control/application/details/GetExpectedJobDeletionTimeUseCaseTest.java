package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.JobStatus;
import org.jobrunr.jobs.Job;
import org.jobrunr.storage.StorageProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetExpectedJobDeletionTimeUseCase")
class GetExpectedJobDeletionTimeUseCaseTest {

    @Mock
    private JobExecutionPort jobExecutionPort;

    @Mock
    private StorageProvider storageProvider;

    @Mock
    private Job job;

    @InjectMocks
    private GetExpectedJobDeletionTimeUseCase useCase;

    @Test
    @DisplayName("returns JobRunr deletion time for succeeded jobs")
    void execute_SucceededJob_ReturnsDeletionTime() {
        UUID jobId = UUID.randomUUID();
        Instant deletionTime = Instant.parse("2026-08-22T03:00:00Z");
        givenExecution(jobId, JobStatus.SUCCEEDED);
        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getDeleteAt()).thenReturn(deletionTime);

        assertThat(useCase.execute(jobId)).contains(deletionTime);
    }

    @Test
    @DisplayName("returns JobRunr deletion time for failed jobs")
    void execute_FailedJob_ReturnsDeletionTime() {
        UUID jobId = UUID.randomUUID();
        Instant deletionTime = Instant.parse("2026-08-23T03:00:00Z");
        givenExecution(jobId, JobStatus.FAILED);
        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getDeleteAt()).thenReturn(deletionTime);

        assertThat(useCase.execute(jobId)).contains(deletionTime);
    }

    @Test
    @DisplayName("does not return deletion time before a terminal status")
    void execute_ProcessingJob_ReturnsEmpty() {
        UUID jobId = UUID.randomUUID();
        givenExecution(jobId, JobStatus.PROCESSING);

        assertThat(useCase.execute(jobId)).isEmpty();
    }

    @Test
    @DisplayName("does not return a value when JobRunr has no deletion scheduled")
    void execute_WithoutDeletionPolicy_ReturnsEmpty() {
        UUID jobId = UUID.randomUUID();
        givenExecution(jobId, JobStatus.FAILED);
        when(storageProvider.getJobById(jobId)).thenReturn(job);
        when(job.getDeleteAt()).thenReturn(null);

        assertThat(useCase.execute(jobId)).isEmpty();
    }

    private void givenExecution(UUID jobId, JobStatus status) {
        JobExecutionInfo executionInfo = new JobExecutionInfo(
                jobId, "Test Job", "TestJob", status,
                Instant.parse("2026-08-20T10:00:00Z"),
                status == JobStatus.SUCCEEDED ? Instant.parse("2026-08-20T10:10:00Z") : null,
                null, Map.of(), Map.of(), null, null, null
        );
        when(jobExecutionPort.getJobChainExecutionById(jobId)).thenReturn(Optional.of(executionInfo));
    }
}

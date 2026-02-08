package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.adapter.rest.dto.BatchProgressDTO;
import ch.css.jobrunr.control.adapter.rest.dto.JobStatusResponse;
import ch.css.jobrunr.control.adapter.rest.dto.StartJobRequestDTO;
import ch.css.jobrunr.control.adapter.rest.dto.StartJobResponse;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionByIdUseCase;
import ch.css.jobrunr.control.application.scheduling.StartJobUseCase;
import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobStatus;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobControlResource")
class JobControlResourceTest {

    @Mock
    private StartJobUseCase startJobUseCase;

    @Mock
    private GetJobExecutionByIdUseCase getJobExecutionByIdUseCase;

    @InjectMocks
    private JobControlResource resource;

    @Test
    @DisplayName("POST /jobs/{jobId}/start should start job and return 200")
    void startJob_ValidRequest_ReturnsSuccess() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        StartJobRequestDTO request = new StartJobRequestDTO(null, Map.of());

        when(startJobUseCase.execute(eq(jobId), any(), any())).thenReturn(resultId);

        // Act
        Response response = resource.startJob(jobId, request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(StartJobResponse.class);

        StartJobResponse startJobResponse = (StartJobResponse) response.getEntity();
        assertThat(startJobResponse.jobId()).isEqualTo(resultId);
        assertThat(startJobResponse.message()).isNotNull();
    }

    @Test
    @DisplayName("POST /jobs/{jobId}/start should throw exception when job not found")
    void startJob_JobNotFound_ThrowsException() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        when(startJobUseCase.execute(eq(jobId), any(), any()))
                .thenThrow(new JobNotFoundException("Job not found: " + jobId));

        // Act & Assert
        assertThatThrownBy(() -> resource.startJob(jobId, new StartJobRequestDTO(null, Map.of())))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    @DisplayName("POST /jobs/{jobId}/start should handle parameter override")
    void startJob_WithParameterOverride_PassesParametersToUseCase() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        Map<String, Object> parameters = Map.of("param1", "value1", "param2", 42);
        StartJobRequestDTO request = new StartJobRequestDTO("test-postfix", parameters);

        when(startJobUseCase.execute(eq(jobId), eq("test-postfix"), eq(parameters)))
                .thenReturn(resultId);

        // Act
        Response response = resource.startJob(jobId, request);

        // Assert
        assertThat(response.getStatus()).isEqualTo(200);
        StartJobResponse startJobResponse = (StartJobResponse) response.getEntity();
        assertThat(startJobResponse.jobId()).isEqualTo(resultId);
    }

    @Test
    @DisplayName("POST /jobs/{jobId}/start with template should return appropriate message")
    void startJob_Template_ReturnsTemplateMessage() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID differentResultId = UUID.randomUUID(); // Different ID indicates template was cloned

        when(startJobUseCase.execute(eq(jobId), any(), any())).thenReturn(differentResultId);

        // Act
        Response response = resource.startJob(jobId, new StartJobRequestDTO(null, Map.of()));

        // Assert
        StartJobResponse startJobResponse = (StartJobResponse) response.getEntity();
        assertThat(startJobResponse.message()).isEqualTo("Template job started successfully");
    }

    @Test
    @DisplayName("POST /jobs/{jobId}/start with regular job should return appropriate message")
    void startJob_RegularJob_ReturnsJobStartedMessage() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        // Same ID indicates regular job (not a template)

        when(startJobUseCase.execute(eq(jobId), any(), any())).thenReturn(jobId);

        // Act
        Response response = resource.startJob(jobId, new StartJobRequestDTO(null, Map.of()));

        // Assert
        StartJobResponse startJobResponse = (StartJobResponse) response.getEntity();
        assertThat(startJobResponse.message()).isEqualTo("Job started successfully");
    }

    @Test
    @DisplayName("POST /jobs/{jobId}/start should throw BadRequestException when jobId is null")
    void startJob_NullJobId_ThrowsBadRequestException() {
        // Act & Assert
        assertThatThrownBy(() -> resource.startJob(null, new StartJobRequestDTO(null, Map.of())))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("jobId is required");
    }

    @Test
    @DisplayName("GET /jobs/{jobId} should return job status")
    void getJobStatus_ValidJobId_ReturnsJobStatus() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        JobExecutionInfo executionInfo = new JobExecutionInfo(
                jobId,
                "Test Job",
                "TestJobType",
                JobStatus.PROCESSING,
                startedAt,
                null,              // finishedAt
                null,              // batchProgress
                Map.of(),          // parameters
                Map.of()           // metadata
        );

        when(getJobExecutionByIdUseCase.execute(jobId)).thenReturn(executionInfo);

        // Act
        Response response = resource.getJobStatus(jobId);

        // Assert
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getEntity()).isInstanceOf(JobStatusResponse.class);

        JobStatusResponse jobStatusResponse = (JobStatusResponse) response.getEntity();
        assertThat(jobStatusResponse.jobId()).isEqualTo(jobId.toString());
        assertThat(jobStatusResponse.jobName()).isEqualTo("Test Job");
        assertThat(jobStatusResponse.jobType()).isEqualTo("TestJobType");
        assertThat(jobStatusResponse.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(jobStatusResponse.startedAt()).isNotNull();
        assertThat(jobStatusResponse.finishedAt()).isNull();
        assertThat(jobStatusResponse.batchProgress()).isNull();
    }

    @Test
    @DisplayName("GET /jobs/{jobId} should throw exception when job not found")
    void getJobStatus_JobNotFound_ThrowsException() {
        // Arrange
        UUID jobId = UUID.randomUUID();

        when(getJobExecutionByIdUseCase.execute(jobId))
                .thenThrow(new JobNotFoundException("Job not found: " + jobId));

        // Act & Assert
        assertThatThrownBy(() -> resource.getJobStatus(jobId))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job not found");
    }

    @Test
    @DisplayName("GET /jobs/{jobId} should include batch progress for batch jobs")
    void getJobStatus_BatchJob_IncludesBatchProgress() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        BatchProgress batchProgress = new BatchProgress(100, 75, 5);

        JobExecutionInfo executionInfo = new JobExecutionInfo(
                jobId,
                "Batch Job",
                "BatchJobType",
                JobStatus.PROCESSING,
                startedAt,
                null,              // finishedAt
                batchProgress,     // batchProgress
                Map.of(),          // parameters
                Map.of()           // metadata
        );

        when(getJobExecutionByIdUseCase.execute(jobId)).thenReturn(executionInfo);

        // Act
        Response response = resource.getJobStatus(jobId);

        // Assert
        JobStatusResponse jobStatusResponse = (JobStatusResponse) response.getEntity();
        assertThat(jobStatusResponse.batchProgress()).isNotNull();

        BatchProgressDTO progressDTO = jobStatusResponse.batchProgress();
        assertThat(progressDTO.total()).isEqualTo(100);
        assertThat(progressDTO.succeeded()).isEqualTo(75);
        assertThat(progressDTO.failed()).isEqualTo(5);
        assertThat(progressDTO.pending()).isEqualTo(20);
        assertThat(progressDTO.progress()).isEqualTo(80.0);
    }
}

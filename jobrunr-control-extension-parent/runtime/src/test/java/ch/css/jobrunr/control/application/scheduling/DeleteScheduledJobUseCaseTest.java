package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteScheduledJobUseCase")
class DeleteScheduledJobUseCaseTest {

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @Mock
    private DeleteParametersUseCase deleteParametersUseCase;

    @InjectMocks
    private DeleteScheduledJobUseCase useCase;

    @Test
    @DisplayName("should delete scheduled job")
    void execute_ValidJobId_DeletesJob() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        ScheduledJobInfo jobInfo = createJobInfo(jobId, false);
        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(jobInfo);

        // Act
        useCase.execute(jobId);

        // Assert
        verify(jobSchedulerPort).getScheduledJobById(jobId);
        verify(jobSchedulerPort).deleteScheduledJob(jobId);
        verify(deleteParametersUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("should delete job and clean up external parameters")
    void execute_JobWithExternalParams_DeletesJobAndParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        UUID paramSetId = UUID.randomUUID();
        Map<String, Object> params = Map.of("__parameterSetId", paramSetId.toString());
        ScheduledJobInfo jobInfo = createJobInfo(jobId, true);

        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(jobInfo);

        // Act
        useCase.execute(jobId);

        // Assert
        verify(deleteParametersUseCase).execute(any(UUID.class));
        verify(jobSchedulerPort).deleteScheduledJob(jobId);
    }

    @Test
    @DisplayName("should throw exception when jobId is null")
    void execute_NullJobId_ThrowsException() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobId");

        verify(jobSchedulerPort, never()).deleteScheduledJob(any());
    }

    // Helper
    private ScheduledJobInfo createJobInfo(UUID jobId, boolean externalParams) {
        Map<String, Object> params = externalParams ?
                Map.of("__parameterSetId", UUID.randomUUID().toString()) :
                Map.of("param1", "value1");
        // Create a minimal job definition
        ch.css.jobrunr.control.domain.JobDefinition jobDef = new ch.css.jobrunr.control.domain.JobDefinition(
                "TestJob", false, "TestJobRequest", "TestJobHandler",
                List.of(),
                new ch.css.jobrunr.control.domain.JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                false, null
        );
        return new ScheduledJobInfo(jobId, "Test Job", jobDef, Instant.now(), params, false, List.of());
    }
}

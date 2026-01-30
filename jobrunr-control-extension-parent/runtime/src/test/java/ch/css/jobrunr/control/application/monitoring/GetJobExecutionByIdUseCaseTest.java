package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetJobExecutionByIdUseCase")
class GetJobExecutionByIdUseCaseTest {

    @Mock
    private JobExecutionPort jobExecutionPort;

    @InjectMocks
    private GetJobExecutionByIdUseCase useCase;

    @Test
    @DisplayName("should return job execution when exists")
    void execute_ExistingExecution_ReturnsExecution() {
        // Arrange
        UUID executionId = UUID.randomUUID();
        JobExecutionInfo expectedExecution = mock(JobExecutionInfo.class);
        when(jobExecutionPort.getJobExecutionById(executionId)).thenReturn(Optional.of(expectedExecution));

        // Act
        JobExecutionInfo result = useCase.execute(executionId);

        // Assert
        assertThat(result).isEqualTo(expectedExecution);
        verify(jobExecutionPort).getJobExecutionById(executionId);
    }

    @Test
    @DisplayName("should throw JobNotFoundException when execution not found")
    void execute_NonExistentExecution_ThrowsException() {
        // Arrange
        UUID executionId = UUID.randomUUID();
        when(jobExecutionPort.getJobExecutionById(executionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(executionId))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job with ID")
                .hasMessageContaining(executionId.toString());
    }
}

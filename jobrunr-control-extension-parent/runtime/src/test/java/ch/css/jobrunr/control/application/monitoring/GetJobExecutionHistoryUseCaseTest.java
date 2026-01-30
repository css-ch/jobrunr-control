package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetJobExecutionHistoryUseCase")
class GetJobExecutionHistoryUseCaseTest {

    @Mock
    private JobExecutionPort jobExecutionPort;

    @InjectMocks
    private GetJobExecutionHistoryUseCase useCase;

    @Test
    @DisplayName("should return execution history")
    void execute_ReturnsExecutionHistory() {
        // Arrange
        List<JobExecutionInfo> expectedHistory = List.of(
                mock(JobExecutionInfo.class),
                mock(JobExecutionInfo.class)
        );
        when(jobExecutionPort.getJobExecutions()).thenReturn(expectedHistory);

        // Act
        List<JobExecutionInfo> result = useCase.execute();

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .isEqualTo(expectedHistory);
        verify(jobExecutionPort).getJobExecutions();
    }

    @Test
    @DisplayName("should return empty list when no executions")
    void execute_NoExecutions_ReturnsEmptyList() {
        // Arrange
        when(jobExecutionPort.getJobExecutions()).thenReturn(List.of());

        // Act
        List<JobExecutionInfo> result = useCase.execute();

        // Assert
        assertThat(result).isEmpty();
    }
}

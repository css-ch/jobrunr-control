package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetScheduledJobByIdUseCase")
class GetScheduledJobByIdUseCaseTest {

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @InjectMocks
    private GetScheduledJobByIdUseCase useCase;

    @Test
    @DisplayName("should return scheduled job when exists")
    void execute_ExistingJob_ReturnsJob() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        ScheduledJobInfo expectedJob = mock(ScheduledJobInfo.class);
        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(expectedJob);

        // Act
        Optional<ScheduledJobInfo> result = useCase.execute(jobId);

        // Assert
        assertThat(result).isPresent().contains(expectedJob);
        verify(jobSchedulerPort).getScheduledJobById(jobId);
    }

    @Test
    @DisplayName("should return empty when job not found")
    void execute_NonExistentJob_ReturnsEmpty() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(null);

        // Act
        Optional<ScheduledJobInfo> result = useCase.execute(jobId);

        // Assert
        assertThat(result).isEmpty();
        verify(jobSchedulerPort).getScheduledJobById(jobId);
    }
}

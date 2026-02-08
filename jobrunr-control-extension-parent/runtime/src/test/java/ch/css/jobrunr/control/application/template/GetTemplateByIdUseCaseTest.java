package ch.css.jobrunr.control.application.template;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTemplateByIdUseCase")
class GetTemplateByIdUseCaseTest {

    @Mock
    private JobSchedulerPort schedulerPort;

    @InjectMocks
    private GetTemplateByIdUseCase useCase;

    @Test
    @DisplayName("should return template when found")
    void shouldReturnTemplateWhenFound() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        ScheduledJobInfo templateJob = mock(ScheduledJobInfo.class);

        when(schedulerPort.getScheduledJobById(templateId))
                .thenReturn(templateJob);
        when(templateJob.isTemplate())
                .thenReturn(true);

        // Act
        Optional<ScheduledJobInfo> result = useCase.execute(templateId);

        // Assert
        assertThat(result)
                .isPresent()
                .contains(templateJob);
    }

    @Test
    @DisplayName("should return empty when job is not a template")
    void shouldReturnEmptyWhenJobIsNotATemplate() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        ScheduledJobInfo regularJob = mock(ScheduledJobInfo.class);

        when(schedulerPort.getScheduledJobById(jobId))
                .thenReturn(regularJob);
        when(regularJob.isTemplate())
                .thenReturn(false);

        // Act
        Optional<ScheduledJobInfo> result = useCase.execute(jobId);

        // Assert
        assertThat(result).isEmpty();
    }
}

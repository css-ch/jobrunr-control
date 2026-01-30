package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.monitoring.GetScheduledJobsUseCase;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSettings;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetTemplatesUseCase")
class GetTemplatesUseCaseTest {

    @Mock
    private GetScheduledJobsUseCase getScheduledJobsUseCase;

    @InjectMocks
    private GetTemplatesUseCase useCase;

    @Test
    @DisplayName("should return all template jobs")
    void execute_ReturnsAllTemplates() {
        // Arrange
        List<ScheduledJobInfo> allJobs = List.of(
                createJobWithLabel("Job1", "template"),
                createJobWithLabel("Job2", "template"),
                createJobWithLabel("Job3", "regular")
        );
        when(getScheduledJobsUseCase.execute()).thenReturn(allJobs);

        // Act
        List<ScheduledJobInfo> result = useCase.execute();

        // Assert
        assertThat(result)
                .hasSize(2)
                .extracting(ScheduledJobInfo::getJobName)
                .containsExactlyInAnyOrder("Job1", "Job2");
    }

    @Test
    @DisplayName("should return empty list when no templates exist")
    void execute_NoTemplates_ReturnsEmptyList() {
        // Arrange
        when(getScheduledJobsUseCase.execute()).thenReturn(List.of());

        // Act
        List<ScheduledJobInfo> result = useCase.execute();

        // Assert
        assertThat(result).isEmpty();
    }

    // Helper
    private ScheduledJobInfo createJobWithLabel(String name, String label) {
        JobDefinition jobDef = new JobDefinition(
                "TestJob", false, "TestJobRequest", "TestJobHandler",
                List.of(),
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                false, null
        );
        return new ScheduledJobInfo(
                UUID.randomUUID(),
                name,
                jobDef,
                Instant.now(),
                Map.of(),
                false,
                List.of(label)
        );
    }
}

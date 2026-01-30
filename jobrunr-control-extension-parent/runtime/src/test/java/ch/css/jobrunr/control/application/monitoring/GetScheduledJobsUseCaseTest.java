package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.*;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetScheduledJobsUseCase")
class GetScheduledJobsUseCaseTest {

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @InjectMocks
    private GetScheduledJobsUseCase useCase;

    private List<ScheduledJobInfo> testJobs;
    private JobDefinition testJobDefinition;

    @BeforeEach
    void setUp() {
        testJobDefinition = createJobDefinition("TestJob");
        testJobs = List.of(
                createScheduledJob("Job1", false),
                createScheduledJob("Job2", true)
        );
    }

    @Test
    @DisplayName("should return all scheduled jobs")
    void execute_ReturnsAllScheduledJobs() {
        // Arrange
        when(jobSchedulerPort.getScheduledJobs()).thenReturn(testJobs);

        // Act
        List<ScheduledJobInfo> result = useCase.execute();

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(testJobs);
        verify(jobSchedulerPort).getScheduledJobs();
    }

    @Test
    @DisplayName("should return empty list when no jobs scheduled")
    void execute_NoJobs_ReturnsEmptyList() {
        // Arrange
        when(jobSchedulerPort.getScheduledJobs()).thenReturn(List.of());

        // Act
        List<ScheduledJobInfo> result = useCase.execute();

        // Assert
        assertThat(result).isEmpty();
        verify(jobSchedulerPort).getScheduledJobs();
    }

    // Test data builders
    private JobDefinition createJobDefinition(String jobType) {
        return new JobDefinition(
                jobType,
                false,
                jobType + "Request",
                jobType + "Handler",
                List.of(new JobParameter("param1", JobParameterType.STRING, true, null, List.of())),
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                false,
                null
        );
    }

    private ScheduledJobInfo createScheduledJob(String jobName, boolean isExternallyTriggerable) {
        return new ScheduledJobInfo(
                UUID.randomUUID(),
                jobName,
                testJobDefinition,
                Instant.now().plusSeconds(3600), // Always provide a valid timestamp
                Map.of("param1", "value1"),
                isExternallyTriggerable,
                List.of()
        );
    }
}

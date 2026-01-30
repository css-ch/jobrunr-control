package ch.css.jobrunr.control.application.discovery;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetJobParametersUseCase")
class GetJobParametersUseCaseTest {

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @InjectMocks
    private GetJobParametersUseCase useCase;

    private JobDefinition testJobDefinition;
    private List<JobParameter> testParameters;

    @BeforeEach
    void setUp() {
        testParameters = List.of(
                new JobParameter("param1", JobParameterType.STRING, true, null, List.of()),
                new JobParameter("param2", JobParameterType.INTEGER, false, "42", List.of())
        );

        testJobDefinition = createJobDefinition("TestJob", testParameters);
    }

    @Test
    @DisplayName("should return parameters for existing job type")
    void execute_ExistingJobType_ReturnsParameters() {
        // Arrange
        String jobType = "TestJob";
        when(jobDefinitionDiscoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));

        // Act
        List<JobParameter> result = useCase.execute(jobType);

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(testParameters);
        verify(jobDefinitionDiscoveryService).findJobByType(jobType);
    }

    @Test
    @DisplayName("should throw JobNotFoundException for non-existent job type")
    void execute_NonExistentJobType_ThrowsJobNotFoundException() {
        // Arrange
        String jobType = "NonExistentJob";
        when(jobDefinitionDiscoveryService.findJobByType(jobType))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(jobType))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("JobDefinition with JobType")
                .hasMessageContaining(jobType)
                .hasMessageContaining("not found");

        verify(jobDefinitionDiscoveryService).findJobByType(jobType);
    }

    @Test
    @DisplayName("should return empty list when job has no parameters")
    void execute_JobWithNoParameters_ReturnsEmptyList() {
        // Arrange
        String jobType = "SimpleJob";
        JobDefinition jobWithNoParams = createJobDefinition(jobType, List.of());
        when(jobDefinitionDiscoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(jobWithNoParams));

        // Act
        List<JobParameter> result = useCase.execute(jobType);

        // Assert
        assertThat(result).isEmpty();
        verify(jobDefinitionDiscoveryService).findJobByType(jobType);
    }

    // Test data builder
    private JobDefinition createJobDefinition(String jobType, List<JobParameter> parameters) {
        return new JobDefinition(
                jobType,
                false,
                jobType + "Request",
                jobType + "Handler",
                parameters,
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                false,
                null
        );
    }
}

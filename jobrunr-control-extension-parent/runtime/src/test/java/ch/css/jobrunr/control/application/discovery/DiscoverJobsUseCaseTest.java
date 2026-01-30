package ch.css.jobrunr.control.application.discovery;

import ch.css.jobrunr.control.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscoverJobsUseCase")
class DiscoverJobsUseCaseTest {

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @InjectMocks
    private DiscoverJobsUseCase useCase;

    private List<JobDefinition> testJobDefinitions;

    @BeforeEach
    void setUp() {
        testJobDefinitions = List.of(
                createJobDefinition("TestJob1", false),
                createJobDefinition("TestJob2", true)
        );
    }

    @Test
    @DisplayName("should return all job definitions")
    void execute_ReturnsAllJobDefinitions() {
        // Arrange
        when(jobDefinitionDiscoveryService.getAllJobDefinitions()).thenReturn(testJobDefinitions);

        // Act
        Collection<JobDefinition> result = useCase.execute();

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(testJobDefinitions);
        verify(jobDefinitionDiscoveryService).getAllJobDefinitions();
    }

    @Test
    @DisplayName("should return empty collection when no jobs defined")
    void execute_NoJobs_ReturnsEmptyCollection() {
        // Arrange
        when(jobDefinitionDiscoveryService.getAllJobDefinitions()).thenReturn(List.of());

        // Act
        Collection<JobDefinition> result = useCase.execute();

        // Assert
        assertThat(result).isEmpty();
        verify(jobDefinitionDiscoveryService).getAllJobDefinitions();
    }

    @Test
    @DisplayName("should delegate to discovery service")
    void execute_DelegatesToDiscoveryService() {
        // Arrange
        when(jobDefinitionDiscoveryService.getAllJobDefinitions()).thenReturn(testJobDefinitions);

        // Act
        useCase.execute();

        // Assert
        verify(jobDefinitionDiscoveryService, times(1)).getAllJobDefinitions();
    }

    // Test data builder
    private JobDefinition createJobDefinition(String jobType, boolean isBatch) {
        return new JobDefinition(
                jobType,
                isBatch,
                jobType + "Request",
                jobType + "Handler",
                List.of(
                        new JobParameter("param1", JobParameterType.STRING, true, null, List.of())
                ),
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                false,
                null
        );
    }
}

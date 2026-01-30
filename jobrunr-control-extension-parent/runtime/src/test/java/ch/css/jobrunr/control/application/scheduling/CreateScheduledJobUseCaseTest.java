package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateScheduledJobUseCase")
class CreateScheduledJobUseCaseTest {

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @Mock
    private JobParameterValidator validator;

    @Mock
    private ParameterStorageHelper parameterStorageHelper;

    @InjectMocks
    private CreateScheduledJobUseCase useCase;

    private JobDefinition testJobDefinition;
    private UUID mockJobId;

    @BeforeEach
    void setUp() {
        mockJobId = UUID.randomUUID();
        testJobDefinition = createJobDefinition("TestJob", false);
    }

    @Test
    @DisplayName("should create scheduled job with valid parameters")
    void execute_ValidParameters_CreatesScheduledJob() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "My Test Job";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        Map<String, Object> jobParams = Map.of("param1", "value1");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(parameterStorageHelper.prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams))
                .thenReturn(jobParams);
        when(jobSchedulerPort.scheduleJob(testJobDefinition, jobName, jobParams, false, scheduledAt, null))
                .thenReturn(mockJobId);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters, scheduledAt, false);

        // Assert
        assertThat(result).isEqualTo(mockJobId);
        verify(jobDefinitionDiscoveryService).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);
        verify(parameterStorageHelper).prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams);
        verify(jobSchedulerPort).scheduleJob(testJobDefinition, jobName, jobParams, false, scheduledAt, null);
    }

    @Test
    @DisplayName("should throw JobNotFoundException for non-existent job type")
    void execute_NonExistentJobType_ThrowsJobNotFoundException() {
        // Arrange
        String jobType = "NonExistentJob";
        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(jobType, "Test", Map.of(), null, false))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(jobType);

        verify(jobDefinitionDiscoveryService).findJobByType(jobType);
        verify(jobSchedulerPort, never()).scheduleJob(any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("should create externally triggerable job")
    void execute_ExternalTrigger_CreatesJobWithoutScheduledTime() {
        // Arrange
        String jobType = "TestJob";
        Map<String, String> parameters = Map.of();
        Map<String, Object> convertedParams = Map.of();
        Map<String, Object> jobParams = Map.of();

        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(parameterStorageHelper.prepareJobParameters(any(), any(), any(), any())).thenReturn(jobParams);
        when(jobSchedulerPort.scheduleJob(any(), any(), any(), eq(true), isNull(), any())).thenReturn(mockJobId);

        // Act
        UUID result = useCase.execute(jobType, "External Job", parameters, null, true);

        // Assert
        assertThat(result).isNotNull();
        verify(jobSchedulerPort).scheduleJob(testJobDefinition, "External Job", jobParams, true, null, null);
    }

    // Test data builder
    private JobDefinition createJobDefinition(String jobType, boolean usesExternalParams) {
        return new JobDefinition(
                jobType,
                false,
                jobType + "Request",
                jobType + "Handler",
                List.of(new JobParameter("param1", JobParameterType.STRING, true, null, List.of())),
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                usesExternalParams,
                usesExternalParams ? "parameterSetId" : null
        );
    }
}

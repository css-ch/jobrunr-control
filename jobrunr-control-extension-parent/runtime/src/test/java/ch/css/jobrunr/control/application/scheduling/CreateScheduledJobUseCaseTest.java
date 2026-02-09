package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
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

    @Mock
    private AuditLoggerHelper auditLogger;

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
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(jobSchedulerPort.scheduleJob(testJobDefinition, jobName, convertedParams, false, scheduledAt, null))
                .thenReturn(mockJobId);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters, scheduledAt, false);

        // Assert
        assertThat(result).isEqualTo(mockJobId);
        verify(jobDefinitionDiscoveryService).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);
        verify(jobSchedulerPort).scheduleJob(testJobDefinition, jobName, convertedParams, false, scheduledAt, null);
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

        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(jobSchedulerPort.scheduleJob(any(), any(), any(), eq(true), isNull(), any())).thenReturn(mockJobId);

        // Act
        UUID result = useCase.execute(jobType, "External Job", parameters, null, true);

        // Assert
        assertThat(result).isEqualTo(mockJobId);
        verify(jobSchedulerPort).scheduleJob(testJobDefinition, "External Job", convertedParams, true, null, null);
    }

    @Test
    @DisplayName("should use two-phase creation for job with external parameters")
    void execute_ExternalParameters_UsesTwoPhaseCreation() {
        // Arrange
        String jobType = "TestJobWithExternalParams";
        String jobName = "My Test Job";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        Map<String, Object> paramReference = Map.of("parameterSetId", mockJobId.toString());
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        JobDefinition externalParamsJobDef = createJobDefinition(jobType, true);

        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.of(externalParamsJobDef));
        when(validator.convertAndValidate(externalParamsJobDef, parameters)).thenReturn(convertedParams);
        when(jobSchedulerPort.scheduleJob(eq(externalParamsJobDef), eq(jobName), eq(Map.of()), eq(false), eq(scheduledAt), isNull()))
                .thenReturn(mockJobId);
        when(parameterStorageHelper.createParameterReference(mockJobId, externalParamsJobDef)).thenReturn(paramReference);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters, scheduledAt, false);

        // Assert
        assertThat(result).isEqualTo(mockJobId);

        // Verify Phase 1: Create job with empty params
        verify(jobSchedulerPort).scheduleJob(externalParamsJobDef, jobName, Map.of(), false, scheduledAt, null);

        // Verify Phase 2: Store parameters with job UUID
        verify(parameterStorageHelper).storeParametersForJob(mockJobId, externalParamsJobDef, jobType, convertedParams);

        // Verify Phase 3: Update job with parameter reference
        verify(parameterStorageHelper).createParameterReference(mockJobId, externalParamsJobDef);
        verify(jobSchedulerPort).updateJobParameters(mockJobId, paramReference);
    }

    @Test
    @DisplayName("should use single-phase creation for job with inline parameters")
    void execute_InlineParameters_UsesSinglePhaseCreation() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "My Test Job";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(jobDefinitionDiscoveryService.findJobByType(jobType)).thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(jobSchedulerPort.scheduleJob(testJobDefinition, jobName, convertedParams, false, scheduledAt, null))
                .thenReturn(mockJobId);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters, scheduledAt, false);

        // Assert
        assertThat(result).isEqualTo(mockJobId);

        // Verify single-phase: Create job with inline params directly
        verify(jobSchedulerPort).scheduleJob(testJobDefinition, jobName, convertedParams, false, scheduledAt, null);

        // Verify no external parameter storage
        verify(parameterStorageHelper, never()).storeParametersForJob(any(), any(), any(), any());
        verify(jobSchedulerPort, never()).updateJobParameters(any(), any());
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

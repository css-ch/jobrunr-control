package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.testutils.JobDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateScheduledJobUseCase")
class UpdateScheduledJobUseCaseTest {

    @Mock
    private JobDefinitionDiscoveryService discoveryService;

    @Mock
    private JobSchedulerPort schedulerPort;

    @Mock
    private JobParameterValidator validator;

    @Mock
    private ParameterStorageHelper storageHelper;

    @Mock
    private ParameterStorageService storageService;

    @Mock
    private AuditLoggerHelper auditLogger;

    @InjectMocks
    private UpdateScheduledJobUseCase useCase;

    private JobDefinition testJobDefinition;

    @BeforeEach
    void setUp() {
        testJobDefinition = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withDisplayName("Test Job")
                .build();
    }

    @Test
    @DisplayName("should update scheduled job successfully with inline parameters")
    void shouldUpdateScheduledJobSuccessfully() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Updated Job";
        Map<String, String> parameters = Map.of("param1", "newValue");
        Map<String, Object> convertedParams = Map.of("param1", "newValue");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(convertedParams);

        // Act
        UUID result = useCase.execute(jobId, jobType, jobName, parameters, scheduledAt, false);

        // Assert
        assertThat(result).isEqualTo(jobId);
        verify(discoveryService).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);

        // For inline parameters, single-phase update with converted params directly
        verify(schedulerPort).updateJob(
                eq(jobId),
                eq(testJobDefinition),
                eq(jobName),
                eq(convertedParams),
                eq(false),
                eq(scheduledAt),
                isNull()
        );

        // No external parameter storage operations
        verify(storageHelper, never()).storeParametersForJob(any(), any(), any(), any());
        verify(schedulerPort, never()).updateJobParameters(any(), any());
    }

    @Test
    @DisplayName("should throw exception when job definition not found")
    void shouldThrowExceptionWhenJobDefinitionNotFound() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String invalidJobType = "NonExistentJob";
        Map<String, String> parameters = Map.of();

        when(discoveryService.findJobByType(invalidJobType))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(
                jobId, invalidJobType, "Job", parameters, Instant.now(), false
        ))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(invalidJobType);

        verify(discoveryService).findJobByType(invalidJobType);
        verifyNoInteractions(validator, storageHelper, storageService, schedulerPort);
    }

    @Test
    @DisplayName("should preserve job type when updating")
    void shouldPreserveJobTypeWhenUpdating() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String jobType = "TestJob";
        Map<String, String> parameters = Map.of();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(any(), any()))
                .thenReturn(Map.of());

        // Act
        useCase.execute(jobId, jobType, "Job", parameters, Instant.now(), false);

        // Assert
        verify(schedulerPort).updateJob(
                eq(jobId),
                eq(testJobDefinition),
                anyString(),
                anyMap(),
                anyBoolean(),
                any(Instant.class),
                isNull()
        );
    }

    @Test
    @DisplayName("should use three-phase update for external parameters")
    void shouldUpdateExternalParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Job with External Params";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        Map<String, Object> paramReference = Map.of("parameterSetId", jobId.toString());
        Instant scheduledAt = Instant.now();

        JobDefinition externalParamJob = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withExternalParameters()
                .build();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(externalParamJob));
        when(validator.convertAndValidate(externalParamJob, parameters))
                .thenReturn(convertedParams);
        when(storageService.isExternalStorageAvailable())
                .thenReturn(true);
        when(storageHelper.createParameterReference(jobId, externalParamJob))
                .thenReturn(paramReference);

        // Act
        useCase.execute(jobId, jobType, jobName, parameters, scheduledAt, false);

        // Assert
        // Verify Phase 1: Delete old parameter set
        verify(storageService).deleteById(jobId);

        // Verify Phase 2: Update job with empty params
        verify(schedulerPort).updateJob(
                eq(jobId),
                eq(externalParamJob),
                eq(jobName),
                eq(Map.of()),  // Empty params in phase 2
                eq(false),
                eq(scheduledAt),
                isNull()
        );

        // Verify Phase 3: Store new parameters with same job UUID
        verify(storageHelper).storeParametersForJob(jobId, externalParamJob, jobType, convertedParams);

        // Verify Phase 4: Update job with parameter reference
        verify(storageHelper).createParameterReference(jobId, externalParamJob);
        verify(schedulerPort).updateJobParameters(jobId, paramReference);
    }
}

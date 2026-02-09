package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
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
    @DisplayName("should update scheduled job successfully")
    void shouldUpdateScheduledJobSuccessfully() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Updated Job";
        Map<String, String> parameters = Map.of("param1", "newValue");
        Map<String, Object> convertedParams = Map.of("param1", "newValue");
        Map<String, Object> jobParams = Map.of("param1", "newValue");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(convertedParams);
        when(storageHelper.prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams))
                .thenReturn(jobParams);

        // Act
        UUID result = useCase.execute(jobId, jobType, jobName, parameters, scheduledAt, false);

        // Assert
        assertThat(result).isEqualTo(jobId);
        verify(discoveryService, atLeastOnce()).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);
        verify(storageHelper).prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams);
        verify(schedulerPort).updateJob(
                eq(jobId),
                eq(testJobDefinition),
                eq(jobName),
                eq(jobParams),
                eq(false),
                eq(scheduledAt),
                isNull()  // additionalLabels is null when not provided
        );
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
                .hasMessageContaining("JobDefinition")
                .hasMessageContaining(invalidJobType);

        verify(discoveryService, atLeastOnce()).findJobByType(invalidJobType);
        verifyNoInteractions(validator, storageHelper, schedulerPort);
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
        when(storageHelper.prepareJobParameters(any(), any(), any(), any()))
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
                isNull()  // additionalLabels is null when not provided
        );
    }

    @Test
    @DisplayName("should update external parameters")
    void shouldUpdateExternalParameters() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Job with External Params";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        UUID paramSetId = UUID.randomUUID();
        Map<String, Object> jobParamsWithSetId = Map.of("_parameterSetId", paramSetId.toString());

        JobDefinition externalParamJob = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withExternalParameters()
                .build();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(externalParamJob));
        when(validator.convertAndValidate(externalParamJob, parameters))
                .thenReturn(convertedParams);
        when(storageHelper.prepareJobParameters(externalParamJob, jobType, jobName, convertedParams))
                .thenReturn(jobParamsWithSetId);

        // Act
        useCase.execute(jobId, jobType, jobName, parameters, Instant.now(), false);

        // Assert
        verify(storageHelper).prepareJobParameters(externalParamJob, jobType, jobName, convertedParams);
        verify(schedulerPort).updateJob(
                any(),
                any(),
                any(),
                eq(jobParamsWithSetId),
                anyBoolean(),
                any(),
                isNull()  // additionalLabels is null when not provided
        );
    }
}

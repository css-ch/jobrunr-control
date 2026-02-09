package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateTemplateUseCase")
class CreateTemplateUseCaseTest {

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
    private CreateTemplateUseCase useCase;

    private JobDefinition testJobDefinition;

    @BeforeEach
    void setUp() {
        testJobDefinition = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withDisplayName("Test Job")
                .build();
    }

    @Test
    @DisplayName("should create template with valid parameters")
    void shouldCreateTemplateWithValidParameters() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "My Template";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        Map<String, Object> jobParams = Map.of("param1", "value1");
        UUID expectedId = UUID.randomUUID();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(convertedParams);
        when(storageHelper.prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams))
                .thenReturn(jobParams);
        when(schedulerPort.scheduleJob(any(), anyString(), any(), anyBoolean(), any(), anyList()))
                .thenReturn(expectedId);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters);

        // Assert
        assertThat(result).isEqualTo(expectedId);

        verify(discoveryService).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);
        verify(storageHelper).prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams);
        verify(schedulerPort).scheduleJob(
                eq(testJobDefinition),
                eq(jobName),
                eq(jobParams),
                eq(true),           // isExternalTrigger
                isNull(),           // scheduledAt - templates have no schedule
                eq(List.of("template"))
        );
    }

    @Test
    @DisplayName("should throw exception when job type not found")
    void shouldThrowExceptionWhenJobTypeNotFound() {
        // Arrange
        String invalidJobType = "NonExistentJob";
        String jobName = "My Template";
        Map<String, String> parameters = Map.of();

        when(discoveryService.findJobByType(invalidJobType))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(invalidJobType, jobName, parameters))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(invalidJobType);

        verify(discoveryService).findJobByType(invalidJobType);
        verifyNoInteractions(validator, storageHelper, schedulerPort);
    }

    @Test
    @DisplayName("should validate template name is not empty")
    void shouldValidateTemplateNameIsNotEmpty() {
        // Arrange
        String jobType = "TestJob";
        String emptyName = "";
        Map<String, String> parameters = Map.of();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(Map.of());
        when(storageHelper.prepareJobParameters(any(), any(), any(), any()))
                .thenReturn(Map.of());
        when(schedulerPort.scheduleJob(any(), anyString(), any(), anyBoolean(), any(), anyList()))
                .thenReturn(UUID.randomUUID());

        // Act
        UUID result = useCase.execute(jobType, emptyName, parameters);

        // Assert - verify empty name is passed through (validation happens in scheduler)
        assertThat(result).isNotNull();
        verify(schedulerPort).scheduleJob(
                any(),
                eq(emptyName),
                any(),
                anyBoolean(),
                any(),
                anyList()
        );
    }

    @Test
    @DisplayName("should store external parameters when configured")
    void shouldStoreExternalParametersWhenConfigured() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "Template with External Params";
        Map<String, String> parameters = Map.of("param1", "value1", "param2", "value2");
        Map<String, Object> convertedParams = Map.of("param1", "value1", "param2", "value2");
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
        when(schedulerPort.scheduleJob(any(), anyString(), any(), anyBoolean(), any(), anyList()))
                .thenReturn(UUID.randomUUID());

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters);

        // Assert
        assertThat(result).isNotNull();
        verify(storageHelper).prepareJobParameters(externalParamJob, jobType, jobName, convertedParams);
        verify(schedulerPort).scheduleJob(
                any(),
                any(),
                eq(jobParamsWithSetId),
                anyBoolean(),
                any(),
                anyList()
        );
    }
}

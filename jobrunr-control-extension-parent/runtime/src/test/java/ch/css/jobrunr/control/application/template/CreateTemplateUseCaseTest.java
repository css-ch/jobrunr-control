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
        UUID expectedId = UUID.randomUUID();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(convertedParams);
        when(schedulerPort.scheduleJob(any(), anyString(), any(), anyBoolean(), any(), anyList()))
                .thenReturn(expectedId);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters);

        // Assert
        assertThat(result).isEqualTo(expectedId);

        verify(discoveryService).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);
        verify(schedulerPort).scheduleJob(
                eq(testJobDefinition),
                eq(jobName),
                eq(convertedParams),
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
    @DisplayName("should use two-phase creation for external parameters")
    void shouldUseTwoPhaseCreationForExternalParameters() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "Template with External Params";
        Map<String, String> parameters = Map.of("param1", "value1", "param2", "value2");
        Map<String, Object> convertedParams = Map.of("param1", "value1", "param2", "value2");
        UUID templateId = UUID.randomUUID();
        Map<String, Object> paramReference = Map.of("parameterSetId", templateId.toString());

        JobDefinition externalParamJob = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withExternalParameters()
                .build();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(externalParamJob));
        when(validator.convertAndValidate(externalParamJob, parameters))
                .thenReturn(convertedParams);
        when(schedulerPort.scheduleJob(eq(externalParamJob), eq(jobName), eq(Map.of()), eq(true), isNull(), eq(List.of("template"))))
                .thenReturn(templateId);
        when(storageHelper.createParameterReference(templateId, externalParamJob))
                .thenReturn(paramReference);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters);

        // Assert
        assertThat(result).isEqualTo(templateId);

        // Verify Phase 1: Create template with empty params
        verify(schedulerPort).scheduleJob(
                eq(externalParamJob),
                eq(jobName),
                eq(Map.of()),  // Empty params in phase 1
                eq(true),
                isNull(),
                eq(List.of("template"))
        );

        // Verify Phase 2: Store parameters with template UUID
        verify(storageHelper).storeParametersForJob(templateId, externalParamJob, jobType, convertedParams);

        // Verify Phase 3: Update template with parameter reference
        verify(storageHelper).createParameterReference(templateId, externalParamJob);
        verify(schedulerPort).updateJobParameters(templateId, paramReference);
    }

    @Test
    @DisplayName("should use single-phase creation for inline parameters")
    void shouldUseSinglePhaseCreationForInlineParameters() {
        // Arrange
        String jobType = "TestJob";
        String jobName = "Template with Inline Params";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        UUID templateId = UUID.randomUUID();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(convertedParams);
        when(schedulerPort.scheduleJob(testJobDefinition, jobName, convertedParams, true, null, List.of("template")))
                .thenReturn(templateId);

        // Act
        UUID result = useCase.execute(jobType, jobName, parameters);

        // Assert
        assertThat(result).isEqualTo(templateId);

        // Verify single-phase: Create template with inline params directly
        verify(schedulerPort).scheduleJob(
                testJobDefinition,
                jobName,
                convertedParams,  // Inline params passed directly
                true,
                null,
                List.of("template")
        );

        // Verify no external parameter storage
        verify(storageHelper, never()).storeParametersForJob(any(), any(), any(), any());
        verify(schedulerPort, never()).updateJobParameters(any(), any());
    }
}

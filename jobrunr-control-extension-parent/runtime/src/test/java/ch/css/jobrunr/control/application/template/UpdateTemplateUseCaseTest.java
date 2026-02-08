package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateTemplateUseCase")
class UpdateTemplateUseCaseTest {

    @Mock
    private JobDefinitionDiscoveryService discoveryService;

    @Mock
    private JobSchedulerPort schedulerPort;

    @Mock
    private JobParameterValidator validator;

    @Mock
    private ParameterStorageHelper storageHelper;

    @InjectMocks
    private UpdateTemplateUseCase useCase;

    private JobDefinition testJobDefinition;

    @BeforeEach
    void setUp() {
        testJobDefinition = new JobDefinitionBuilder()
                .withJobType("TestJob")
                .withDisplayName("Test Job")
                .build();
    }

    @Test
    @DisplayName("should update template with valid parameters")
    void shouldUpdateTemplateWithValidParameters() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Updated Template";
        Map<String, String> parameters = Map.of("param1", "newValue");
        Map<String, Object> convertedParams = Map.of("param1", "newValue");
        Map<String, Object> jobParams = Map.of("param1", "newValue");

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(testJobDefinition, parameters))
                .thenReturn(convertedParams);
        when(storageHelper.prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams))
                .thenReturn(jobParams);

        // Act
        useCase.execute(templateId, jobType, jobName, parameters);

        // Assert
        verify(discoveryService, atLeastOnce()).findJobByType(jobType);
        verify(validator).convertAndValidate(testJobDefinition, parameters);
        verify(storageHelper).prepareJobParameters(testJobDefinition, jobType, jobName, convertedParams);
        verify(schedulerPort).updateJob(
                eq(templateId),
                eq(testJobDefinition),
                eq(jobName),
                eq(jobParams),
                eq(true),           // isExternalTrigger
                any(),              // EXTERNAL_TRIGGER_DATE
                eq(List.of("template"))
        );
    }

    @Test
    @DisplayName("should throw exception when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        String invalidJobType = "NonExistentJob";
        String jobName = "Template";
        Map<String, String> parameters = Map.of();

        when(discoveryService.findJobByType(invalidJobType))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(templateId, invalidJobType, jobName, parameters))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("JobDefinition")
                .hasMessageContaining(invalidJobType);

        verify(discoveryService, atLeastOnce()).findJobByType(invalidJobType);
        verifyNoInteractions(validator, storageHelper, schedulerPort);
    }

    @Test
    @DisplayName("should preserve template flag when updating")
    void shouldPreserveTemplateFlagWhenUpdating() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Template";
        Map<String, String> parameters = Map.of();

        when(discoveryService.findJobByType(jobType))
                .thenReturn(Optional.of(testJobDefinition));
        when(validator.convertAndValidate(any(), any()))
                .thenReturn(Map.of());
        when(storageHelper.prepareJobParameters(any(), any(), any(), any()))
                .thenReturn(Map.of());

        // Act
        useCase.execute(templateId, jobType, jobName, parameters);

        // Assert - verify "template" label is always included
        verify(schedulerPort).updateJob(
                any(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                eq(List.of("template"))
        );
    }

    @Test
    @DisplayName("should update external parameters")
    void shouldUpdateExternalParameters() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Template with External Params";
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
        useCase.execute(templateId, jobType, jobName, parameters);

        // Assert
        verify(storageHelper).prepareJobParameters(externalParamJob, jobType, jobName, convertedParams);
        verify(schedulerPort).updateJob(
                any(),
                any(),
                any(),
                eq(jobParamsWithSetId),
                anyBoolean(),
                any(),
                anyList()
        );
    }
}

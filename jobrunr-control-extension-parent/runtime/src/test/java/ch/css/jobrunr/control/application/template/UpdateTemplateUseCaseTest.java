package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.scheduling.ParameterStorageHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.exceptions.DuplicateTemplateNameException;
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

    @Mock
    private AuditLoggerHelper auditLogger;

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
    @DisplayName("should update template with inline parameters")
    void shouldUpdateTemplateWithValidParameters() {
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "Updated Template";
        Map<String, String> parameters = Map.of("param1", "newValue");
        Map<String, Object> convertedParams = Map.of("param1", "newValue");

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);

        useCase.execute(templateId, jobType, jobName, parameters);

        verify(storageHelper).updateJobWithParameters(
                eq(templateId), eq(testJobDefinition), eq(jobType), eq(jobName),
                eq(convertedParams), eq(true), isNull(), eq(List.of("template")));
        verify(auditLogger).logTemplateUpdated(jobName, templateId, convertedParams);
    }

    @Test
    @DisplayName("should throw exception when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
        UUID templateId = UUID.randomUUID();
        String invalidJobType = "NonExistentJob";

        when(discoveryService.requireJobByType(invalidJobType))
                .thenThrow(new JobNotFoundException("Job type '" + invalidJobType + "' not found"));

        assertThatThrownBy(() -> useCase.execute(templateId, invalidJobType, "Template", Map.of()))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(invalidJobType);

        verifyNoInteractions(validator, storageHelper, auditLogger);
    }

    @Test
    @DisplayName("should throw DuplicateTemplateNameException when name taken by another template")
    void shouldThrowWhenNameTakenByOtherTemplate() {
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";
        String duplicateName = "Existing Template";

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        doThrow(new DuplicateTemplateNameException(duplicateName))
                .when(schedulerPort).assertTemplateNameUnique(eq(duplicateName), eq(templateId));

        assertThatThrownBy(() -> useCase.execute(templateId, jobType, duplicateName, Map.of()))
                .isInstanceOf(DuplicateTemplateNameException.class);

        verify(storageHelper, never()).updateJobWithParameters(any(), any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("should allow keeping the same name for the same template")
    void shouldAllowSameNameForSameTemplate() {
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";
        String jobName = "My Template";

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(any(), any())).thenReturn(Map.of());

        useCase.execute(templateId, jobType, jobName, Map.of());

        verify(storageHelper).updateJobWithParameters(
                eq(templateId), any(), any(), any(), any(), eq(true),
                isNull(), eq(List.of("template")));
    }

    @Test
    @DisplayName("should always update with isExternalTrigger=true and template label")
    void shouldAlwaysUseExternalTriggerAndTemplateLabel() {
        UUID templateId = UUID.randomUUID();
        String jobType = "TestJob";

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(any(), any())).thenReturn(Map.of());

        useCase.execute(templateId, jobType, "Template", Map.of());

        verify(storageHelper).updateJobWithParameters(
                any(), any(), any(), any(), any(),
                eq(true),               // always external trigger
                isNull(),               // helper resolves EXTERNAL_TRIGGER_DATE
                eq(List.of("template")) // always template label
        );
    }
}

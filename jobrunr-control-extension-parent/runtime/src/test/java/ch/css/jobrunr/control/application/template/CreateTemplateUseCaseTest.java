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
        String jobType = "TestJob";
        String jobName = "My Template";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        UUID expectedId = UUID.randomUUID();

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(storageHelper.scheduleJobWithParameters(
                eq(testJobDefinition), eq(jobType), eq(jobName), eq(convertedParams),
                eq(true), isNull(), eq(List.of("template"))))
                .thenReturn(expectedId);

        UUID result = useCase.execute(jobType, jobName, parameters);

        assertThat(result).isEqualTo(expectedId);
        verify(storageHelper).scheduleJobWithParameters(
                testJobDefinition, jobType, jobName, convertedParams, true, null, List.of("template"));
        verify(auditLogger).logTemplateCreated(jobName, expectedId, convertedParams);
    }

    @Test
    @DisplayName("should throw exception when job type not found")
    void shouldThrowExceptionWhenJobTypeNotFound() {
        String invalidJobType = "NonExistentJob";
        when(discoveryService.requireJobByType(invalidJobType))
                .thenThrow(new JobNotFoundException("Job type '" + invalidJobType + "' not found"));

        assertThatThrownBy(() -> useCase.execute(invalidJobType, "My Template", Map.of()))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(invalidJobType);

        verifyNoInteractions(validator, storageHelper, auditLogger);
    }

    @Test
    @DisplayName("should throw DuplicateTemplateNameException when name already exists")
    void shouldThrowWhenTemplateNameAlreadyExists() {
        String jobType = "TestJob";
        String duplicateName = "Existing Template";

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        doThrow(new DuplicateTemplateNameException(duplicateName))
                .when(schedulerPort).assertTemplateNameUnique(eq(duplicateName), isNull());

        assertThatThrownBy(() -> useCase.execute(jobType, duplicateName, Map.of()))
                .isInstanceOf(DuplicateTemplateNameException.class);

        verify(storageHelper, never()).scheduleJobWithParameters(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("should always schedule with isExternalTrigger=true and template label")
    void shouldAlwaysUseExternalTriggerAndTemplateLabel() {
        String jobType = "TestJob";
        String jobName = "My Template";
        Map<String, Object> convertedParams = Map.of();
        UUID templateId = UUID.randomUUID();

        when(discoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(any(), any())).thenReturn(convertedParams);
        when(storageHelper.scheduleJobWithParameters(any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(templateId);

        useCase.execute(jobType, jobName, Map.of());

        verify(storageHelper).scheduleJobWithParameters(
                any(), any(), any(), any(),
                eq(true),               // always external trigger
                isNull(),               // no scheduled time
                eq(List.of("template")) // always template label
        );
    }
}

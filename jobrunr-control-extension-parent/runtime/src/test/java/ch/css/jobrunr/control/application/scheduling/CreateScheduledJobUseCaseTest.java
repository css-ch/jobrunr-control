package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
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
        String jobType = "TestJob";
        String jobName = "My Test Job";
        Map<String, String> parameters = Map.of("param1", "value1");
        Map<String, Object> convertedParams = Map.of("param1", "value1");
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(parameterStorageHelper.scheduleJobWithParameters(
                eq(testJobDefinition), eq(jobType), eq(jobName), eq(convertedParams), eq(false), eq(scheduledAt), isNull()))
                .thenReturn(mockJobId);

        UUID result = useCase.execute(jobType, jobName, parameters, scheduledAt, false);

        assertThat(result).isEqualTo(mockJobId);
        verify(parameterStorageHelper).scheduleJobWithParameters(
                testJobDefinition, jobType, jobName, convertedParams, false, scheduledAt, null);
        verify(auditLogger).logJobCreated(jobName, mockJobId, convertedParams);
    }

    @Test
    @DisplayName("should throw JobNotFoundException for non-existent job type")
    void execute_NonExistentJobType_ThrowsJobNotFoundException() {
        String jobType = "NonExistentJob";
        when(jobDefinitionDiscoveryService.requireJobByType(jobType))
                .thenThrow(new JobNotFoundException("Job type '" + jobType + "' not found"));

        assertThatThrownBy(() -> useCase.execute(jobType, "Test", Map.of(), null, false))
                .isInstanceOf(JobNotFoundException.class)
                .hasMessageContaining("Job type")
                .hasMessageContaining(jobType);

        verify(parameterStorageHelper, never()).scheduleJobWithParameters(any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    @DisplayName("should create externally triggerable job")
    void execute_ExternalTrigger_CreatesJobWithoutScheduledTime() {
        String jobType = "TestJob";
        Map<String, Object> convertedParams = Map.of();

        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(testJobDefinition, Map.of())).thenReturn(convertedParams);
        when(parameterStorageHelper.scheduleJobWithParameters(any(), any(), any(), any(), eq(true), isNull(), any()))
                .thenReturn(mockJobId);

        UUID result = useCase.execute(jobType, "External Job", Map.of(), null, true);

        assertThat(result).isEqualTo(mockJobId);
        verify(parameterStorageHelper).scheduleJobWithParameters(
                testJobDefinition, jobType, "External Job", convertedParams, true, null, null);
    }

    @Test
    @DisplayName("should pass additional labels to scheduleJobWithParameters")
    void execute_WithAdditionalLabels_PassesLabelsThrough() {
        String jobType = "TestJob";
        String jobName = "Labelled Job";
        List<String> labels = List.of("production");
        Map<String, String> parameters = Map.of();
        Map<String, Object> convertedParams = Map.of();
        Instant scheduledAt = Instant.now().plusSeconds(3600);

        when(jobDefinitionDiscoveryService.requireJobByType(jobType)).thenReturn(testJobDefinition);
        when(validator.convertAndValidate(testJobDefinition, parameters)).thenReturn(convertedParams);
        when(parameterStorageHelper.scheduleJobWithParameters(any(), any(), any(), any(), anyBoolean(), any(), eq(labels)))
                .thenReturn(mockJobId);

        UUID result = useCase.execute(jobType, jobName, parameters, scheduledAt, false, labels);

        assertThat(result).isEqualTo(mockJobId);
        verify(parameterStorageHelper).scheduleJobWithParameters(
                testJobDefinition, jobType, jobName, convertedParams, false, scheduledAt, labels);
    }

    // Test data builder
    private JobDefinition createJobDefinition(String jobType, boolean usesExternalParams) {
        return new JobDefinition(
                jobType,
                false,
                jobType + "Request",
                jobType + "Handler",
                List.of(new JobParameter("param1", "param1", null, JobParameterType.STRING, true, null, List.of(), 0, "default")),
                List.of(),
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                usesExternalParams,
                usesExternalParams ? "parameterSetId" : null
        );
    }
}

package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.template.TemplateCloneHelper;
import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.testutils.JobDefinitionBuilder;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Use case level equivalent of JobTriggerForParameterDemoJobUITest.
 * Tests the create → trigger flow directly via use cases without UI or REST layer.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Job create and trigger flow for ParameterDemoJob")
class JobTriggerForParameterDemoJobUseCaseTest {

    private static final String JOB_TYPE = "ParameterDemoJob";
    private static final String JOB_NAME = "Test Job - External Trigger";

    @Mock
    private JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @Mock
    private JobParameterValidator validator;

    @Mock
    private ParameterStorageHelper parameterStorageHelper;

    @Mock
    @SuppressWarnings("unused")
    private AuditLoggerHelper auditLogger;

    @Mock
    private TemplateCloneHelper templateCloneHelper;

    @InjectMocks
    private CreateScheduledJobUseCase createUseCase;

    private StartJobUseCase startUseCase;

    private JobDefinition parameterDemoJobDefinition;
    private UUID scheduledJobId;

    @BeforeEach
    void setUp() {
        startUseCase = new StartJobUseCase(jobSchedulerPort, templateCloneHelper, auditLogger);
        scheduledJobId = UUID.randomUUID();

        parameterDemoJobDefinition = new JobDefinitionBuilder()
                .withJobType(JOB_TYPE)
                .withDisplayName("Parameter Demo Job")
                .withParameters(List.of(
                        new JobParameter("stringParameter", "stringParameter", null, JobParameterType.STRING, true, "Default String", List.of(), 0, null),
                        new JobParameter("multilineParameter", "multilineParameter", null, JobParameterType.MULTILINE, true, "Line 1\nLine 2\nLine 3", List.of(), 1, null),
                        new JobParameter("integerParameter", "integerParameter", null, JobParameterType.INTEGER, true, "42", List.of(), 2, null),
                        new JobParameter("doubleParameter", "doubleParameter", null, JobParameterType.DOUBLE, true, "3.14159", List.of(), 3, null),
                        new JobParameter("booleanParameter", "booleanParameter", null, JobParameterType.BOOLEAN, true, "true", List.of(), 4, null),
                        new JobParameter("dateParameter", "dateParameter", null, JobParameterType.DATE, true, "2024-01-01", List.of(), 5, null),
                        new JobParameter("dateTimeParameter", "dateTimeParameter", null, JobParameterType.DATETIME, true, "2024-01-01T12:00:00", List.of(), 6, null),
                        new JobParameter("enumParameter", "enumParameter", null, JobParameterType.ENUM, true, "OPTION_B", List.of(), 7, null),
                        new JobParameter("multiEnumParameter", "multiEnumParameter", null, JobParameterType.MULTI_ENUM, true, "OPTION_A,OPTION_C", List.of(), 8, null)
                ))
                .build();
    }

    @Test
    @DisplayName("should create job with external trigger")
    void step1_createJobWithExternalTrigger() {
        Map<String, String> inputParams = Map.of(
                "stringParameter", "Default String",
                "integerParameter", "42"
        );
        Map<String, Object> convertedParams = Map.of(
                "stringParameter", "Default String",
                "integerParameter", 42
        );

        when(jobDefinitionDiscoveryService.findJobByType(JOB_TYPE)).thenReturn(Optional.of(parameterDemoJobDefinition));
        when(validator.convertAndValidate(parameterDemoJobDefinition, inputParams)).thenReturn(convertedParams);
        when(jobSchedulerPort.scheduleJob(eq(parameterDemoJobDefinition), eq(JOB_NAME), eq(convertedParams), eq(true), isNull(), isNull()))
                .thenReturn(scheduledJobId);

        UUID result = createUseCase.execute(JOB_TYPE, JOB_NAME, inputParams, null, true);

        assertThat(result).isEqualTo(scheduledJobId);
        verify(jobSchedulerPort).scheduleJob(parameterDemoJobDefinition, JOB_NAME, convertedParams, true, null, null);
    }

    @Test
    @DisplayName("should trigger job via start use case with parameter override")
    void step2_triggerJobWithParameterOverride() {
        Map<String, Object> overrides = Map.of("test-parameter", "yes");

        ScheduledJobInfo scheduledJob = new ScheduledJobInfo(
                scheduledJobId, JOB_NAME, parameterDemoJobDefinition,
                Instant.now(), Map.of(), true, List.of()
        );
        when(jobSchedulerPort.getScheduledJobById(scheduledJobId)).thenReturn(scheduledJob);

        UUID result = startUseCase.execute(scheduledJobId, "-testrun", overrides);

        assertThat(result).isEqualTo(scheduledJobId);
        verify(jobSchedulerPort).executeJobNow(scheduledJobId, overrides);
        verifyNoInteractions(templateCloneHelper);
    }

    @Test
    @DisplayName("should create job with external trigger and then start it with parameter override")
    void createAndTriggerJobWithParameterOverride() {
        Map<String, String> inputParams = Map.of(
                "stringParameter", "Default String",
                "integerParameter", "42"
        );
        Map<String, Object> convertedParams = Map.of(
                "stringParameter", "Default String",
                "integerParameter", 42
        );
        Map<String, Object> overrides = Map.of("test-parameter", "yes");

        when(jobDefinitionDiscoveryService.findJobByType(JOB_TYPE)).thenReturn(Optional.of(parameterDemoJobDefinition));
        when(validator.convertAndValidate(parameterDemoJobDefinition, inputParams)).thenReturn(convertedParams);
        when(jobSchedulerPort.scheduleJob(any(), any(), any(), eq(true), isNull(), isNull()))
                .thenReturn(scheduledJobId);

        ScheduledJobInfo scheduledJob = new ScheduledJobInfo(
                scheduledJobId, JOB_NAME, parameterDemoJobDefinition,
                Instant.now(), Map.of(), true, List.of()
        );
        when(jobSchedulerPort.getScheduledJobById(scheduledJobId)).thenReturn(scheduledJob);

        // Step 1: Create
        UUID createdId = createUseCase.execute(JOB_TYPE, JOB_NAME, inputParams, null, true);
        assertThat(createdId).isEqualTo(scheduledJobId);
        verify(jobSchedulerPort).scheduleJob(parameterDemoJobDefinition, JOB_NAME, convertedParams, true, null, null);

        // Step 2: Trigger
        UUID triggeredId = startUseCase.execute(createdId, "-testrun", overrides);
        assertThat(triggeredId).isEqualTo(scheduledJobId);
        verify(jobSchedulerPort).executeJobNow(scheduledJobId, overrides);
    }
}

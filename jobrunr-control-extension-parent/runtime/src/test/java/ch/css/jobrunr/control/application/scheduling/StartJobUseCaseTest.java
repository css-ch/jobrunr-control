package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.template.TemplateCloneHelper;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.audit.TriggerSource;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StartJobUseCase")
class StartJobUseCaseTest {

    @Mock
    private JobSchedulerPort jobSchedulerPort;

    @Mock
    private TemplateCloneHelper templateCloneHelper;

    @Mock
    private AuditLoggerHelper auditLogger;

    private StartJobUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StartJobUseCase(jobSchedulerPort, templateCloneHelper, auditLogger);
    }

    @Test
    @DisplayName("should start regular job directly using port")
    void execute_RegularJob_StartsDirectly() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        Map<String, Object> parameters = Map.of("key", "value");
        String postfix = "test-postfix";

        ScheduledJobInfo regularJob = createJob(jobId, "RegularJob", List.of());
        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(regularJob);

        // Act
        UUID result = useCase.execute(jobId, postfix, parameters);

        // Assert
        assertThat(result).isEqualTo(jobId);
        verify(jobSchedulerPort).executeJobNow(jobId, parameters);
        verifyNoInteractions(templateCloneHelper);
        verify(auditLogger).logJobExecuted("RegularJob", jobId, TriggerSource.UI);
    }

    @Test
    @DisplayName("should clone and start template job using port and helper")
    void execute_TemplateJob_ClonesAndStarts() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        UUID newJobId = UUID.randomUUID();
        Map<String, Object> parameters = Map.of("key", "value");
        String postfix = "test-postfix";

        ScheduledJobInfo templateJob = createJob(templateId, "TemplateJob", List.of("template"));
        when(jobSchedulerPort.getScheduledJobById(templateId)).thenReturn(templateJob);
        when(templateCloneHelper.cloneTemplate(templateId, postfix, parameters, null))
                .thenReturn(newJobId);

        // Act
        UUID result = useCase.execute(templateId, postfix, parameters);

        // Assert
        assertThat(result).isEqualTo(newJobId);
        verify(templateCloneHelper).cloneTemplate(templateId, postfix, parameters, null);
        Map<String, Object> expectedMetadata = new java.util.HashMap<>(parameters);
        expectedMetadata.put("templateName", "TemplateJob");
        verify(jobSchedulerPort).executeJobNow(newJobId, expectedMetadata);
        verify(auditLogger).logTemplateExecuted("TemplateJob", templateId, newJobId, TriggerSource.UI);
    }

    @Test
    @DisplayName("should add only templateName to metadata when parameterOverrides is null")
    void execute_TemplateJob_NullParameters_AddsOnlyTemplateName() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        UUID newJobId = UUID.randomUUID();

        ScheduledJobInfo templateJob = createJob(templateId, "MyTemplate", List.of("template"));
        when(jobSchedulerPort.getScheduledJobById(templateId)).thenReturn(templateJob);
        when(templateCloneHelper.cloneTemplate(templateId, null, null, null)).thenReturn(newJobId);

        // Act
        UUID result = useCase.execute(templateId, null, null);

        // Assert
        assertThat(result).isEqualTo(newJobId);
        verify(jobSchedulerPort).executeJobNow(newJobId, Map.of("templateName", "MyTemplate"));
        verify(auditLogger).logTemplateExecuted("MyTemplate", templateId, newJobId, TriggerSource.UI);
    }

    @Test
    @DisplayName("should handle null postfix and parameters")
    void execute_NullPostfixAndParameters_HandlesCorrectly() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        ScheduledJobInfo regularJob = createJob(jobId, "RegularJob", List.of());
        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(regularJob);

        // Act
        UUID result = useCase.execute(jobId, null, null);

        // Assert
        assertThat(result).isEqualTo(jobId);
        verify(jobSchedulerPort).executeJobNow(jobId, null);
    }

    @Test
    @DisplayName("should throw NotFoundException when job not found")
    void execute_JobNotFound_ThrowsException() {
        // Arrange
        UUID jobId = UUID.randomUUID();
        when(jobSchedulerPort.getScheduledJobById(jobId)).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(jobId, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Job with ID " + jobId + " not found");

        verify(jobSchedulerPort, never()).executeJobNow(any(), any());
        verifyNoInteractions(templateCloneHelper);
        verifyNoInteractions(auditLogger);
    }

    private ScheduledJobInfo createJob(UUID jobId, String jobName, List<String> labels) {
        JobDefinition jobDef = new JobDefinition(
                "TestJob", false, "TestJobRequest", "TestJobHandler",
                List.of(), List.of(),
                new JobSettings("", false, 3, List.of(), List.of(), "", "", "", "", "", "", ""),
                false, null
        );
        return new ScheduledJobInfo(
                jobId,
                jobName,
                jobDef,
                Instant.now(),
                Map.of(),
                false,
                labels
        );
    }
}

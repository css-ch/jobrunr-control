package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteTemplateUseCase")
class DeleteTemplateUseCaseTest {

    @Mock
    private JobSchedulerPort schedulerPort;

    @Mock
    private ParameterStoragePort parameterStoragePort;

    @Mock
    @SuppressWarnings("unused") // injected for audit logging, not directly relevant for these tests
    private AuditLoggerHelper auditLogger;

    @InjectMocks
    private DeleteTemplateUseCase useCase;

    @Test
    @DisplayName("should delete template by ID")
    void shouldDeleteTemplateById() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        ScheduledJobInfo jobInfo = mock(ScheduledJobInfo.class);

        when(schedulerPort.getScheduledJobById(templateId))
                .thenReturn(jobInfo);
        when(jobInfo.hasExternalParameters())
                .thenReturn(false);

        // Act
        useCase.execute(templateId);

        // Assert
        verify(schedulerPort).getScheduledJobById(templateId);
        verify(schedulerPort).deleteScheduledJob(templateId);
        verify(parameterStoragePort, never()).deleteById(any());
    }

    @Test
    @DisplayName("should throw exception when template ID is null")
    void shouldThrowExceptionWhenTemplateIdIsNull() {
        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId must not be null");

        verifyNoInteractions(schedulerPort, parameterStoragePort);
    }

    @Test
    @DisplayName("should cleanup external parameters on deletion using template ID as parameter set ID")
    void shouldCleanupExternalParametersOnDeletion() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        ScheduledJobInfo jobInfo = mock(ScheduledJobInfo.class);

        when(schedulerPort.getScheduledJobById(templateId))
                .thenReturn(jobInfo);
        when(jobInfo.hasExternalParameters())
                .thenReturn(true);
        when(jobInfo.getParameterSetId())
                .thenReturn(Optional.of(templateId));

        // Act
        useCase.execute(templateId);

        // Assert
        verify(parameterStoragePort).deleteById(templateId);
        verify(schedulerPort).deleteScheduledJob(templateId);
    }
}

package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteScheduledJobUseCase")
class DeleteScheduledJobUseCaseTest {

    @Mock
    private DeleteJobHelper deleteJobHelper;

    @Mock
    private AuditLoggerHelper auditLogger;

    @InjectMocks
    private DeleteScheduledJobUseCase useCase;

    @Test
    @DisplayName("should delete scheduled job via helper")
    void execute_ValidJobId_DelegatesToHelper() {
        UUID jobId = UUID.randomUUID();

        useCase.execute(jobId);

        verify(deleteJobHelper).deleteJobWithCleanup(eq(jobId), any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    @DisplayName("should throw exception when jobId is null")
    void execute_NullJobId_ThrowsException() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("jobId");

        verifyNoInteractions(deleteJobHelper, auditLogger);
    }

    @Test
    @DisplayName("should throw exception when confirmed is false")
    void execute_NotConfirmed_ThrowsException() {
        UUID jobId = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(jobId, false))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmed");

        verifyNoInteractions(deleteJobHelper, auditLogger);
    }

    @Test
    @DisplayName("should delete when confirmed is true")
    void execute_Confirmed_DelegatesToHelper() {
        UUID jobId = UUID.randomUUID();

        useCase.execute(jobId, true);

        verify(deleteJobHelper).deleteJobWithCleanup(eq(jobId), any());
    }
}

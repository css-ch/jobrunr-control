package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.application.audit.AuditLoggerHelper;
import ch.css.jobrunr.control.application.scheduling.DeleteJobHelper;
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
@DisplayName("DeleteTemplateUseCase")
class DeleteTemplateUseCaseTest {

    @Mock
    private DeleteJobHelper deleteJobHelper;

    @Mock
    private AuditLoggerHelper auditLogger;

    @InjectMocks
    private DeleteTemplateUseCase useCase;

    @Test
    @DisplayName("should delete template via helper")
    void shouldDeleteTemplateById() {
        UUID templateId = UUID.randomUUID();

        useCase.execute(templateId);

        verify(deleteJobHelper).deleteJobWithCleanup(eq(templateId), any());
        verifyNoInteractions(auditLogger);
    }

    @Test
    @DisplayName("should throw exception when template ID is null")
    void shouldThrowExceptionWhenTemplateIdIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("templateId must not be null");

        verifyNoInteractions(deleteJobHelper, auditLogger);
    }
}

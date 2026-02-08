package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExecuteTemplateUseCase")
class ExecuteTemplateUseCaseTest {

    @Mock
    private JobSchedulerPort schedulerPort;

    @Mock
    private TemplateCloneHelper templateCloneHelper;

    @InjectMocks
    private ExecuteTemplateUseCase useCase;

    @Test
    @DisplayName("should execute template without parameter override")
    void shouldExecuteTemplateWithoutParameterOverride() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        UUID clonedJobId = UUID.randomUUID();
        String postfix = "exec";

        when(templateCloneHelper.cloneTemplate(any(), any(), any(), any()))
                .thenReturn(clonedJobId);

        // Act
        UUID result = useCase.execute(templateId, postfix, null);

        // Assert
        assertThat(result).isEqualTo(clonedJobId);
        verify(templateCloneHelper).cloneTemplate(
                eq(templateId),
                eq(postfix),
                isNull(),
                isNull()  // No "template" label
        );
        verify(schedulerPort).executeJobNow(clonedJobId, null);
    }

    @Test
    @DisplayName("should execute template with parameter override")
    void shouldExecuteTemplateWithParameterOverride() {
        // Arrange
        UUID templateId = UUID.randomUUID();
        UUID clonedJobId = UUID.randomUUID();
        String postfix = "exec";
        Map<String, Object> paramOverrides = Map.of("param1", "overridden");

        when(templateCloneHelper.cloneTemplate(any(), any(), any(), any()))
                .thenReturn(clonedJobId);

        // Act
        UUID result = useCase.execute(templateId, postfix, paramOverrides);

        // Assert
        assertThat(result).isEqualTo(clonedJobId);
        verify(templateCloneHelper).cloneTemplate(
                eq(templateId),
                eq(postfix),
                eq(paramOverrides),
                isNull()
        );
        verify(schedulerPort).executeJobNow(clonedJobId, paramOverrides);
    }

    @Test
    @DisplayName("should throw exception when template not found")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Arrange
        UUID nonExistentTemplateId = UUID.randomUUID();
        String postfix = "exec";

        when(templateCloneHelper.cloneTemplate(any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Template not found"));

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentTemplateId, postfix, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template not found");
    }
}

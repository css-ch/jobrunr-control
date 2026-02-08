package ch.css.jobrunr.control.application.template;

import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloneTemplateUseCase")
class CloneTemplateUseCaseTest {

    @Mock
    private TemplateCloneHelper templateCloneHelper;

    @InjectMocks
    private CloneTemplateUseCase useCase;

    @Test
    @DisplayName("should clone template with new name")
    void shouldCloneTemplateWithNewName() {
        // Arrange
        UUID sourceTemplateId = UUID.randomUUID();
        UUID clonedTemplateId = UUID.randomUUID();
        String postfix = "copy";

        when(templateCloneHelper.cloneTemplate(any(), any(), any(), anyList()))
                .thenReturn(clonedTemplateId);

        // Act
        UUID result = useCase.execute(sourceTemplateId, postfix);

        // Assert
        assertThat(result).isEqualTo(clonedTemplateId);
        verify(templateCloneHelper).cloneTemplate(
                eq(sourceTemplateId),
                eq(postfix),
                isNull(),                   // No parameter overrides for template clones
                eq(java.util.List.of("template"))
        );
    }

    @Test
    @DisplayName("should throw exception when source template not found")
    void shouldThrowExceptionWhenSourceTemplateNotFound() {
        // Arrange
        UUID nonExistentTemplateId = UUID.randomUUID();
        String postfix = "copy";

        when(templateCloneHelper.cloneTemplate(any(), any(), any(), anyList()))
                .thenThrow(new JobNotFoundException("Template not found: " + nonExistentTemplateId));

        // Act & Assert
        assertThatThrownBy(() -> useCase.execute(nonExistentTemplateId, postfix))
                .isInstanceOf(JobNotFoundException.class);

        verify(templateCloneHelper).cloneTemplate(
                eq(nonExistentTemplateId),
                anyString(),
                any(),
                anyList()
        );
    }
}

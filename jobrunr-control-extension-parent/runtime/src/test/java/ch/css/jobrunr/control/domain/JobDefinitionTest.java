package ch.css.jobrunr.control.domain;

import ch.css.jobrunr.control.testutils.JobDefinitionBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JobDefinition")
class JobDefinitionTest {

    @Test
    @DisplayName("should create with all fields")
    void shouldCreateWithAllFields() {
        // Arrange
        JobParameter param1 = new JobParameter("param1", JobParameterType.STRING, true, null, List.of());
        JobParameter param2 = new JobParameter("param2", JobParameterType.INTEGER, false, "10", List.of());
        List<JobParameter> parameters = List.of(param1, param2);
        JobSettings settings = new JobSettings("Test", false, 3, List.of(), List.of(), "", "", "", "", "", "", "");

        // Act
        JobDefinition definition = new JobDefinition(
                "TestJob",
                false,
                "com.example.TestJobRequest",
                "com.example.TestJobHandler",
                parameters,
                settings,
                false,
                null
        );

        // Assert
        assertThat(definition.jobType()).isEqualTo("TestJob");
        assertThat(definition.isBatchJob()).isFalse();
        assertThat(definition.jobRequestTypeName()).isEqualTo("com.example.TestJobRequest");
        assertThat(definition.handlerClassName()).isEqualTo("com.example.TestJobHandler");
        assertThat(definition.parameters()).hasSize(2);
        assertThat(definition.jobSettings()).isEqualTo(settings);
        assertThat(definition.usesExternalParameters()).isFalse();
        assertThat(definition.parameterSetFieldName()).isNull();
    }

    @Test
    @DisplayName("usesExternalParameters should return correct value")
    void usesExternalParametersShouldReturnCorrectValue() {
        // Arrange - job with inline parameters
        JobDefinition inlineJob = new JobDefinitionBuilder()
                .withJobType("InlineJob")
                .build();

        // Arrange - job with external parameters
        JobDefinition externalJob = new JobDefinitionBuilder()
                .withJobType("ExternalJob")
                .withExternalParameters()
                .build();

        // Assert
        assertThat(inlineJob.usesExternalParameters()).isFalse();
        assertThat(externalJob.usesExternalParameters()).isTrue();
    }

    @Test
    @DisplayName("getParameterNames should return list of parameter names")
    void getParameterNamesShouldReturnListOfNames() {
        // Arrange
        JobParameter param1 = new JobParameter("firstName", JobParameterType.STRING, true, null, List.of());
        JobParameter param2 = new JobParameter("age", JobParameterType.INTEGER, false, "18", List.of());
        JobParameter param3 = new JobParameter("email", JobParameterType.STRING, true, null, List.of());

        JobDefinition definition = new JobDefinitionBuilder()
                .withParameters(List.of(param1, param2, param3))
                .build();

        // Act
        List<String> parameterNames = definition.getParameterNames();

        // Assert
        assertThat(parameterNames).containsExactly("firstName", "age", "email");
    }
}

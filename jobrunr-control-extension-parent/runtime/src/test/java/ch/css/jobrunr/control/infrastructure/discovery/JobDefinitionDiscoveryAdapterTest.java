package ch.css.jobrunr.control.infrastructure.discovery;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import ch.css.jobrunr.control.domain.JobSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JobDefinitionDiscoveryAdapter")
class JobDefinitionDiscoveryAdapterTest {

    private JobDefinitionDiscoveryAdapter adapter;

    @BeforeEach
    void setUp() {
        // Initialize the registry with test data
        JobDefinition testJob1 = createJobDefinition("TestJob1", false);
        JobDefinition testJob2 = createJobDefinition("TestJob2", true);
        JobDefinition testJob3 = createJobDefinition("ExternalParamJob", false, true);

        JobDefinitionRecorder.JobDefinitionRegistry.INSTANCE.setDefinitions(
                Set.of(testJob1, testJob2, testJob3)
        );

        adapter = new JobDefinitionDiscoveryAdapter();
    }

    @Test
    @DisplayName("should return all registered job definitions")
    void getAllJobDefinitions_ReturnsAllRegisteredDefinitions() {
        // Act
        Collection<JobDefinition> definitions = adapter.getAllJobDefinitions();

        // Assert
        assertThat(definitions)
                .isNotNull()
                .hasSize(3)
                .allMatch(def -> def.jobType() != null)
                .allMatch(def -> def.handlerClassName() != null);
    }

    @Test
    @DisplayName("should find job by type when job exists")
    void findJobByType_ExistingJob_ReturnsJobDefinition() {
        // Act
        Optional<JobDefinition> result = adapter.findJobByType("TestJob1");

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .satisfies(jobDef -> {
                    assertThat(jobDef.jobType()).isEqualTo("TestJob1");
                    assertThat(jobDef.handlerClassName()).isEqualTo("com.example.TestJob1Handler");
                    assertThat(jobDef.isBatchJob()).isFalse();
                });
    }

    @Test
    @DisplayName("should return empty optional when job type not found")
    void findJobByType_NonExistingJob_ReturnsEmpty() {
        // Act
        Optional<JobDefinition> result = adapter.findJobByType("NonExistentJob");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty optional when job type is null")
    void findJobByType_NullJobType_ReturnsEmpty() {
        // Act
        Optional<JobDefinition> result = adapter.findJobByType(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return consistent results on multiple calls")
    void getAllJobDefinitions_MultipleCalls_ReturnsConsistentResults() {
        // Act
        Collection<JobDefinition> firstCall = adapter.getAllJobDefinitions();
        Collection<JobDefinition> secondCall = adapter.getAllJobDefinitions();

        // Assert
        assertThat(firstCall)
                .isNotNull()
                .hasSameSizeAs(secondCall)
                .containsExactlyInAnyOrderElementsOf(secondCall);
    }

    @Test
    @DisplayName("should delegate to JobDefinitionRegistry")
    void getAllJobDefinitions_DelegatesToRegistry() {
        // Act
        Collection<JobDefinition> fromAdapter = adapter.getAllJobDefinitions();
        Collection<JobDefinition> fromRegistry = JobDefinitionRecorder.JobDefinitionRegistry.INSTANCE.getAllDefinitions();

        // Assert
        assertThat(fromAdapter)
                .isNotNull()
                .containsExactlyInAnyOrderElementsOf(fromRegistry);
    }

    @Test
    @DisplayName("should find batch job by type")
    void findJobByType_BatchJob_ReturnsCorrectDefinition() {
        // Act
        Optional<JobDefinition> result = adapter.findJobByType("TestJob2");

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .satisfies(jobDef -> {
                    assertThat(jobDef.jobType()).isEqualTo("TestJob2");
                    assertThat(jobDef.isBatchJob()).isTrue();
                });
    }

    @Test
    @DisplayName("should find job with external parameters")
    void findJobByType_ExternalParameters_ReturnsCorrectDefinition() {
        // Act
        Optional<JobDefinition> result = adapter.findJobByType("ExternalParamJob");

        // Assert
        assertThat(result)
                .isPresent()
                .get()
                .satisfies(jobDef -> {
                    assertThat(jobDef.jobType()).isEqualTo("ExternalParamJob");
                    assertThat(jobDef.usesExternalParameters()).isTrue();
                });
    }

    // Test data builders
    private JobDefinition createJobDefinition(String jobType, boolean isBatch) {
        return createJobDefinition(jobType, isBatch, false);
    }

    private JobDefinition createJobDefinition(String jobType, boolean isBatch, boolean externalParams) {
        return new JobDefinition(
                jobType,                                    // jobType
                isBatch,                                    // isBatchJob
                "com.example." + jobType + "Request",       // jobRequestTypeName
                "com.example." + jobType + "Handler",       // handlerClassName
                List.of(                                    // parameters
                        new JobParameter("param1", JobParameterType.STRING, false, null, List.of(), 0),
                        new JobParameter("param2", JobParameterType.INTEGER, true, "42", List.of(), 1)
                ),
                new JobSettings(                            // jobSettings
                        jobType + " Job",                   // name
                        isBatch,                            // isBatch
                        5,                                  // retries
                        List.of(),                          // labels
                        List.of(),                          // jobFilters
                        null,                               // queue
                        null,                               // runOnServerWithTag
                        null,                               // mutex
                        null,                               // rateLimiter
                        null,                               // processTimeOut
                        null,                               // deleteOnSuccess
                        null                                // deleteOnFailure
                ),
                externalParams,                             // usesExternalParameters
                externalParams ? "parameterSet" : null      // parameterSetFieldName
        );
    }
}

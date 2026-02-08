package ch.css.jobrunr.control.testutils;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Test data builder for JobDefinition.
 * Provides fluent API for creating JobDefinition instances in tests.
 */
public class JobDefinitionBuilder {

    private String jobType = "TestJob";
    private boolean isBatch = false;
    private String displayName = "Test Job";
    private String className = "ch.css.test.TestJob";
    private List<JobParameter> parameters = new ArrayList<>();
    private JobSettings settings = createDefaultSettings();
    private boolean usesExternalParameters = false;
    private String parameterSetId = null;

    public JobDefinitionBuilder withJobType(String jobType) {
        this.jobType = jobType;
        return this;
    }

    public JobDefinitionBuilder asBatchJob() {
        this.isBatch = true;
        return this;
    }

    public JobDefinitionBuilder withDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public JobDefinitionBuilder withClassName(String className) {
        this.className = className;
        return this;
    }

    public JobDefinitionBuilder withParameters(List<JobParameter> parameters) {
        this.parameters = new ArrayList<>(parameters);
        return this;
    }

    public JobDefinitionBuilder addParameter(JobParameter parameter) {
        this.parameters.add(parameter);
        return this;
    }

    public JobDefinitionBuilder withExternalParameters() {
        this.usesExternalParameters = true;
        this.settings = new JobSettings(
                "Test Job",
                true,  // usesExternalParameterStorage
                3,     // maxRetries
                List.of(),
                List.of(),
                "",    // cronExpression
                "",    // fixedDelayExpression
                "",    // fixedRateExpression
                "",    // initialDelayExpression
                "",    // zoneId
                "",    // amountOfRetriesExpression
                ""     // retryBackOffTimeSeedExpression
        );
        return this;
    }

    public JobDefinitionBuilder withSettings(JobSettings settings) {
        this.settings = settings;
        return this;
    }

    public JobDefinition build() {
        return new JobDefinition(
                jobType,
                isBatch,
                displayName,
                className,
                parameters,
                settings,
                usesExternalParameters,
                parameterSetId
        );
    }

    private JobSettings createDefaultSettings() {
        return new JobSettings(
                "Test Job",
                false,  // usesExternalParameterStorage
                3,      // maxRetries
                List.of(),
                List.of(),
                "",     // cronExpression
                "",     // fixedDelayExpression
                "",     // fixedRateExpression
                "",     // initialDelayExpression
                "",     // zoneId
                "",     // amountOfRetriesExpression
                ""      // retryBackOffTimeSeedExpression
        );
    }
}

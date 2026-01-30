package ch.css.jobrunr.control.domain;

import java.util.List;

/**
 * Represents a job definition with metadata about parameters and execution settings.
 *
 * @param jobType                The simple class name of the job handler
 * @param isBatchJob             Whether this job processes batches
 * @param jobRequestTypeName     Fully qualified name of the JobRequest class
 * @param handlerClassName       Fully qualified name of the JobRequestHandler class
 * @param parameters             List of job parameters (inline or external schema)
 * @param jobSettings            Job execution settings from @ConfigurableJob
 * @param usesExternalParameters Whether this job uses external parameter storage (@JobParameterSet)
 * @param parameterSetFieldName  Name of the field annotated with @JobParameterSet (null if inline)
 */
public record JobDefinition(String jobType,
                            boolean isBatchJob,
                            String jobRequestTypeName,
                            String handlerClassName,
                            List<JobParameter> parameters,
                            JobSettings jobSettings,
                            boolean usesExternalParameters,
                            String parameterSetFieldName) {

    @SuppressWarnings("unused") // Used in type-safe qute templates
    public List<String> getParameterNames() {
        return parameters.stream()
                .map(JobParameter::name)
                .toList();
    }
}

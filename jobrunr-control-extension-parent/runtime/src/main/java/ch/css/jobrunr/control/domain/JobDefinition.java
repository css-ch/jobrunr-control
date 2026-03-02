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
 * @param parameterSections      List of parameter sections for grouping parameters in the UI (at minimum a default section)
 * @param jobSettings            Job execution settings from @ConfigurableJob
 * @param usesExternalParameters Whether this job uses external parameter storage (@JobParameterSet)
 * @param externalParametersClassName  Name of the class used for external parameter storage (if usesExternalParameters is true)
 */
public record JobDefinition(String jobType,
                            boolean isBatchJob,
                            String jobRequestTypeName,
                            String handlerClassName,
                            List<JobParameter> parameters,
                            List<JobParameterSection> parameterSections,
                            JobSettings jobSettings,
                            boolean usesExternalParameters,
                            String externalParametersClassName) {

    @SuppressWarnings("unused") // Used in type-safe qute templates
    public List<String> getParameterNames() {
        return parameters.stream()
                .map(JobParameter::name)
                .toList();
    }
}

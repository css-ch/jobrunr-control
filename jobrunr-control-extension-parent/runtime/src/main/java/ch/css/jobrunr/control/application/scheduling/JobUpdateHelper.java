package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.application.validation.JobParameterValidator;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;

import java.util.Map;
import java.util.Optional;

public final class JobUpdateHelper {
    private JobUpdateHelper() {
    }

    /**
     * Loads JobDefinition, validates parameters, and prepares job parameters.
     * Throws JobNotFoundException if job definition is not found.
     *
     * @param jobType                       Job type (class name)
     * @param jobName                       Job name
     * @param parameters                    Parameter map
     * @param jobDefinitionDiscoveryService Service to discover job definition
     * @param validator                     Parameter validator
     * @param parameterStorageHelper        Parameter storage helper
     * @return JobUpdateData containing JobDefinition and prepared parameters
     */
    public static JobUpdateData prepareJobUpdateData(
            String jobType,
            String jobName,
            Map<String, String> parameters,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService,
            JobParameterValidator validator,
            ParameterStorageHelper parameterStorageHelper
    ) {
        Optional<JobDefinition> jobDefOpt = jobDefinitionDiscoveryService.findJobByType(jobType);
        if (jobDefOpt.isEmpty()) {
            throw new JobNotFoundException("JobDefinition for type '" + jobType + "' not found");
        }
        JobDefinition jobDefinition = jobDefOpt.get();
        Map<String, Object> convertedParameters = validator.convertAndValidate(jobDefinition, parameters);
        Map<String, Object> jobParameters = parameterStorageHelper.prepareJobParameters(
                jobDefinition, jobType, jobName, convertedParameters);
        return new JobUpdateData(jobDefinition, jobParameters);
    }

    public record JobUpdateData(JobDefinition jobDefinition, Map<String, Object> jobParameters) {
    }
}

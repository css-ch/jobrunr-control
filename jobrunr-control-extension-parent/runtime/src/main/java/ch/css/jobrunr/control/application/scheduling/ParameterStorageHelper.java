package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * Helper class to prepare job parameters for scheduling.
 * Handles the distinction between inline and external parameter storage.
 */
@ApplicationScoped
public class ParameterStorageHelper {

    private static final Logger log = Logger.getLogger(ParameterStorageHelper.class);

    private final ParameterStorageService parameterStorageService;

    @Inject
    public ParameterStorageHelper(ParameterStorageService parameterStorageService) {
        this.parameterStorageService = parameterStorageService;
    }

    /**
     * Prepares job parameters for scheduling.
     * If the job uses external parameters (@JobParameterSet annotation), stores them externally
     * and returns a parameter set ID reference. Otherwise, returns the converted parameters directly.
     *
     * @param jobDefinition       The job definition containing parameter metadata
     * @param jobType             The job type (for external storage identification)
     * @param jobName             The job name (for logging purposes)
     * @param convertedParameters The validated and converted parameters
     * @return Map of parameters ready for job scheduling
     * @throws IllegalStateException if job requires external storage but it's not configured
     */
    public Map<String, Object> prepareJobParameters(
            JobDefinition jobDefinition,
            String jobType,
            String jobName,
            Map<String, Object> convertedParameters) {

        if (!jobDefinition.usesExternalParameters()) {
            // INLINE: Use converted parameters directly
            return convertedParameters;
        }

        // Validate external storage is available
        if (!parameterStorageService.isExternalStorageAvailable()) {
            throw new IllegalStateException(
                    "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                            "but external storage is not configured. " +
                            "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
        }

        // Store parameters externally
        UUID parameterSetId = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, jobType, convertedParameters);
        parameterStorageService.store(parameterSet);

        log.infof("Stored parameters externally with ID: %s for job: %s", parameterSetId, jobName);

        // Return parameter map with only the parameter set ID
        return Map.of(jobDefinition.parameterSetFieldName(), parameterSetId.toString());
    }
}

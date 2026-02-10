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

    private static final Logger LOG = Logger.getLogger(ParameterStorageHelper.class);

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
        // NOTE: This method is kept for backward compatibility or cases where job ID is not yet known
        // but ideally we should use storeParametersForJob with the job UUID
        UUID parameterSetId = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, jobType, convertedParameters);
        parameterStorageService.store(parameterSet);

        LOG.infof("Stored parameters externally with ID: %s for job: %s", parameterSetId, jobName);

        // Return parameter map with only the parameter set ID
        return Map.of(jobDefinition.parameterSetFieldName(), parameterSetId.toString());
    }

    /**
     * Stores parameters for a job that has already been created.
     * Used for jobs with external parameter storage after the job UUID is known.
     *
     * @param jobId               The UUID of the already-created job
     * @param jobDefinition       The job definition
     * @param jobType             The job type (for storage identification)
     * @param convertedParameters The validated and converted parameters
     */
    public void storeParametersForJob(
            UUID jobId,
            JobDefinition jobDefinition,
            String jobType,
            Map<String, Object> convertedParameters) {

        if (!jobDefinition.usesExternalParameters()) {
            return; // Nothing to store
        }

        if (!parameterStorageService.isExternalStorageAvailable()) {
            throw new IllegalStateException(
                    "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                            "but external storage is not configured. " +
                            "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
        }

        // Use job UUID as parameter set ID
        ParameterSet parameterSet = ParameterSet.create(jobId, jobType, convertedParameters);
        parameterStorageService.store(parameterSet);

        LOG.infof("Stored parameters externally with ID: %s (job UUID) for job type: %s", jobId, jobType);
    }

    /**
     * Updates parameters for an existing job.
     * Used when updating a job that already has an external parameter set.
     *
     * @param jobId               The UUID of the job
     * @param jobDefinition       The job definition
     * @param jobType             The job type (for storage identification)
     * @param convertedParameters The validated and converted parameters
     */
    public void updateParametersForJob(
            UUID jobId,
            JobDefinition jobDefinition,
            String jobType,
            Map<String, Object> convertedParameters) {

        if (!jobDefinition.usesExternalParameters()) {
            return; // Nothing to update
        }

        if (!parameterStorageService.isExternalStorageAvailable()) {
            throw new IllegalStateException(
                    "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                            "but external storage is not configured. " +
                            "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
        }

        ParameterSet parameterSet = ParameterSet.create(jobId, jobType, convertedParameters);
        parameterStorageService.update(parameterSet);

        LOG.infof("Updated parameters externally with ID: %s (job UUID) for job type: %s", jobId, jobType);
    }

    /**
     * Prepares a reference map to the parameter set for jobs with external parameters.
     * Returns empty map for inline parameters, or a map with the parameter set field reference.
     *
     * @param jobId         The job UUID (used as parameter set ID reference)
     * @param jobDefinition The job definition
     * @return Map with parameter set reference, or empty map for inline parameters
     */
    public Map<String, Object> createParameterReference(UUID jobId, JobDefinition jobDefinition) {
        if (!jobDefinition.usesExternalParameters()) {
            return Map.of(); // Empty for inline parameters
        }

        return Map.of(jobDefinition.parameterSetFieldName(), jobId.toString());
    }
}

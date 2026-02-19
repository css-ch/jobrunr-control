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
     * Validates that external storage is available for jobs that require it.
     *
     * @param jobDefinition The job definition
     * @param jobType       The job type (for error messaging)
     * @throws IllegalStateException if external storage is required but not available
     */
    private void validateExternalStorage(JobDefinition jobDefinition, String jobType) {
        if (!jobDefinition.usesExternalParameters()) {
            return;
        }

        if (!parameterStorageService.isExternalStorageAvailable()) {
            throw new IllegalStateException(
                    "Job '" + jobType + "' requires external parameter storage (@JobParameterSet), " +
                            "but external storage is not configured. " +
                            "Enable Hibernate ORM: quarkus.hibernate-orm.enabled=true");
        }
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

        validateExternalStorage(jobDefinition, jobType);

        if (!jobDefinition.usesExternalParameters()) {
            return; // Nothing to store
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

        validateExternalStorage(jobDefinition, jobType);

        if (!jobDefinition.usesExternalParameters()) {
            return; // Nothing to update
        }

        ParameterSet parameterSet = ParameterSet.create(jobId, jobType, convertedParameters);
        parameterStorageService.update(parameterSet);

        LOG.infof("Updated parameters externally with ID: %s (job UUID) for job type: %s", jobId, jobType);
    }
}

package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.filters.ApplyStateFilter;
import org.jobrunr.jobs.states.JobState;
import org.jobrunr.jobs.states.StateName;

import java.util.Map;
import java.util.UUID;

/**
 * JobRunr filter that cleans up external parameter sets when jobs are deleted.
 * <p>
 * This filter intercepts job state changes and deletes the associated parameter set
 * when a job transitions to DELETED state.
 */
@Singleton
public class ParameterCleanupJobFilter implements ApplyStateFilter {

    private static final Logger log = Logger.getLogger(ParameterCleanupJobFilter.class);
    private static final String PARAMETER_SET_ID_KEY = "__parameterSetId";

    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public ParameterCleanupJobFilter(ParameterStoragePort parameterStoragePort) {
        this.parameterStoragePort = parameterStoragePort;
    }

    @Override
    public void onStateApplied(Job job, JobState oldState, JobState newState) {
        // Check if job is being deleted
        if (newState.getName() == StateName.DELETED) {
            cleanupParametersIfExists(job);
        }
    }

    /**
     * Checks if the job has external parameters and deletes them if found.
     */
    private void cleanupParametersIfExists(Job job) {
        try {
            // Extract parameter set ID from job metadata
            Map<String, Object> metadata = job.getMetadata();
            Object paramSetIdObj = metadata.get(PARAMETER_SET_ID_KEY);

            if (paramSetIdObj == null) {
                // Check job details for parameter set ID
                // JobRunr stores job parameters in the job details
                paramSetIdObj = extractParameterSetIdFromJobDetails(job);
            }

            if (paramSetIdObj != null) {
                UUID parameterSetId = parseParameterSetId(paramSetIdObj);
                if (parameterSetId != null) {
                    log.debugf("Deleting external parameters for deleted job %s: %s",
                            job.getId(), parameterSetId);
                    parameterStoragePort.deleteById(parameterSetId);
                    log.infof("Deleted parameter set %s for job %s", parameterSetId, job.getId());
                }
            }
        } catch (Exception e) {
            log.warnf(e, "Failed to cleanup parameters for job %s", job.getId());
            // Don't throw - we don't want to prevent job deletion
        }
    }

    /**
     * Extracts parameter set ID from job details (job parameters).
     */
    private Object extractParameterSetIdFromJobDetails(Job job) {
        try {
            // Job parameters are stored in job details
            // For JobRequest pattern, parameters might be in the first argument
            var jobDetails = job.getJobDetails();
            if (jobDetails.getJobParameters() != null && !jobDetails.getJobParameters().isEmpty()) {
                var firstParam = jobDetails.getJobParameters().getFirst();
                if (firstParam != null && firstParam.getObject() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> paramMap = (Map<String, Object>) firstParam.getObject();
                    return paramMap.get(PARAMETER_SET_ID_KEY);
                }
            }
        } catch (Exception e) {
            log.debugf("Could not extract parameter set ID from job details: %s", e.getMessage());
        }
        return null;
    }

    /**
     * Parses parameter set ID from various object types.
     */
    private UUID parseParameterSetId(Object paramSetIdObj) {
        if (paramSetIdObj instanceof UUID) {
            return (UUID) paramSetIdObj;
        } else if (paramSetIdObj instanceof String) {
            try {
                return UUID.fromString((String) paramSetIdObj);
            } catch (IllegalArgumentException e) {
                log.warnf("Invalid UUID string: %s", paramSetIdObj);
                return null;
            }
        }
        return null;
    }
}

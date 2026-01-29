package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Utility class for extracting job parameters from JobRunr jobs.
 * Handles both regular parameters and JobRequest objects.
 * Uses Jackson ObjectMapper to serialize JobRequest objects to Map.
 */
public class JobParameterExtractor {

    private static final Logger log = Logger.getLogger(JobParameterExtractor.class);

    private JobParameterExtractor() {
        // Utility class
    }

    /**
     * Extracts parameters from a JobRunr job.
     * If there is only one parameter and it is a JobRequest, its fields are returned directly as parameters.
     * If the job uses external parameter storage (@JobParameterSet), the external parameters are loaded
     * and combined with the parameter set ID for display.
     * Otherwise, the parameters are returned as a map.
     *
     * @param job the JobRunr job
     * @return a map of parameter names to values (enriched with external parameters if applicable)
     */
    public static Map<String, Object> extractParameters(org.jobrunr.jobs.Job job) {
        Map<String, Object> parameters = new HashMap<>();
        try {
            var jobDetails = job.getJobDetails();
            var jobParameters = jobDetails.getJobParameters();
            if (jobParameters.size() == 1) {
                Object param = jobParameters.getFirst().getObject();
                if (param instanceof JobRequest jobRequest) {
                    Map<String, Object> unwrapped = unwrapJobRequest(jobRequest);

                    // NEW: Check if this uses external parameters and enrich
                    return enrichWithExternalParameters(unwrapped, job);
                } else {
                    parameters.put("param0", param);
                    return parameters;
                }
            } else {
                for (int i = 0; i < jobParameters.size(); i++) {
                    Object param = jobParameters.get(i).getObject();
                    parameters.put("param" + i, param);
                }
                return parameters;
            }
        } catch (Exception e) {
            log.warnf(e, "Error extracting job parameters for job %s", job.getId());
            return parameters;
        }
    }

    /**
     * Enriches parameters with external parameter set if present.
     * Detects if the unwrapped parameters contain a UUID (parameter set ID),
     * loads the external parameters, and combines them for display.
     *
     * @param unwrapped the unwrapped JobRequest parameters
     * @param job       the JobRunr job
     * @return enriched parameters (external params + parameter set ID field)
     */
    private static Map<String, Object> enrichWithExternalParameters(
            Map<String, Object> unwrapped, org.jobrunr.jobs.Job job) {

        // Find parameter set ID (look for UUID-formatted string value)
        Map.Entry<String, Object> paramSetEntry = findParameterSetIdEntry(unwrapped);
        if (paramSetEntry == null) {
            return unwrapped; // No external parameters
        }

        String parameterSetId = (String) paramSetEntry.getValue();

        // Load external parameters
        try {
            ParameterStorageService storageService =
                    CDI.current().select(ParameterStorageService.class).get();

            Optional<ParameterSet> parameterSet =
                    storageService.findById(UUID.fromString(parameterSetId));

            if (parameterSet.isEmpty()) {
                log.warnf("Parameter set %s not found for job %s", parameterSetId, job.getId());
                return unwrapped;
            }

            // Combine: external parameters + parameter set ID field for reference
            Map<String, Object> combined = new HashMap<>(parameterSet.get().parameters());
            combined.put(paramSetEntry.getKey(), parameterSetId);

            // Update last accessed
            storageService.updateLastAccessed(UUID.fromString(parameterSetId));

            log.debugf("Enriched job %s with %d external parameters from set %s",
                    job.getId(), parameterSet.get().parameters().size(), parameterSetId);

            return combined;

        } catch (Exception e) {
            log.warnf(e, "Failed to load external parameters for job %s", job.getId());
            return unwrapped;
        }
    }

    /**
     * Finds the entry that contains a parameter set ID (UUID string).
     * Scans all entries and returns the first one with a valid UUID value.
     *
     * @param unwrapped the unwrapped parameters
     * @return the entry containing a UUID, or null if none found
     */
    private static Map.Entry<String, Object> findParameterSetIdEntry(Map<String, Object> unwrapped) {
        for (Map.Entry<String, Object> entry : unwrapped.entrySet()) {
            if (entry.getValue() instanceof String str) {
                try {
                    UUID.fromString(str);
                    return entry; // Found valid UUID
                } catch (IllegalArgumentException e) {
                    // Not a UUID, continue
                }
            }
        }
        return null;
    }

    /**
     * Unwraps a JobRequest object into a map of field names to values.
     * Uses Jackson ObjectMapper to convert the JobRequest to a Map.
     */
    private static Map<String, Object> unwrapJobRequest(JobRequest jobRequest) {
        ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();
        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.convertValue(jobRequest, Map.class);
        result.remove("jobRequestHandler");
        return result;
    }
}

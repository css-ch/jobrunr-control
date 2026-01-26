package ch.css.jobrunr.control.infrastructure.jobrunr;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.HashMap;
import java.util.Map;

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
     * Otherwise, the parameters are returned as a map.
     *
     * @param job the JobRunr job
     * @return a map of parameter names to values (unwrapped if single JobRequest)
     */
    public static Map<String, Object> extractParameters(org.jobrunr.jobs.Job job) {
        Map<String, Object> parameters = new HashMap<>();
        try {
            var jobDetails = job.getJobDetails();
            var jobParameters = jobDetails.getJobParameters();
            if (jobParameters.size() == 1) {
                Object param = jobParameters.getFirst().getObject();
                if (param instanceof JobRequest jobRequest) {
                    return unwrapJobRequest(jobRequest);
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

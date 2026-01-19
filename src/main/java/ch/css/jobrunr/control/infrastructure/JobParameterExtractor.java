package ch.css.jobrunr.control.infrastructure;

import org.jobrunr.jobs.lambdas.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for extracting job parameters from JobRunr jobs.
 * Handles both regular parameters and JobRequest objects.
 */
public class JobParameterExtractor {

    private static final Logger log = LoggerFactory.getLogger(JobParameterExtractor.class);

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
                if (isJobRequest(param)) {
                    return unwrapJobRequest(param);
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
            log.warn("Error extracting job parameters for job {}", job.getId(), e);
            return parameters;
        }
    }

    private static boolean isJobRequest(Object obj) {
        return obj instanceof JobRequest;
    }

    private static Map<String, Object> unwrapJobRequest(Object jobRequest) {
        Map<String, Object> map = new HashMap<>();
        Class<?> clazz = jobRequest.getClass();
        while (clazz != null && clazz != Object.class) {
            for (var field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    map.put(field.getName(), field.get(jobRequest));
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return map;
    }
}

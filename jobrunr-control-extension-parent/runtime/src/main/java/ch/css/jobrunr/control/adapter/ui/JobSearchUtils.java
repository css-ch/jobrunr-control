package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;

import java.util.List;
import java.util.Map;

/**
 * Utility class for searching jobs by name, type, and parameters.
 */
public final class JobSearchUtils {

    private JobSearchUtils() {
    }

    /**
     * Applies search filter to a list of job executions.
     * Searches in job name, job type, and parameters (if search contains "=").
     */
    public static List<JobExecutionInfo> applySearchToExecutions(String search, List<JobExecutionInfo> executions) {
        if (search == null || search.isBlank()) {
            return executions;
        }

        final String searchLower = search.toLowerCase();
        return executions.stream()
                .filter(e -> e.getJobName().toLowerCase().contains(searchLower) ||
                        e.getJobType().toLowerCase().contains(searchLower) ||
                        matchesParameters(search, e.getParameters()))
                .toList();
    }

    /**
     * Applies search filter to a list of scheduled jobs.
     * Searches in job name, job type, and parameters (if search contains "=").
     */
    public static List<ScheduledJobInfo> applySearchToScheduledJobs(String search, List<ScheduledJobInfo> jobs) {
        if (search == null || search.isBlank()) {
            return jobs;
        }

        final String searchLower = search.toLowerCase();
        return jobs.stream()
                .filter(j -> j.jobName().toLowerCase().contains(searchLower) ||
                        j.jobDefinition().jobType().toLowerCase().contains(searchLower) ||
                        matchesParameters(search, j.parameters()))
                .toList();
    }

    /**
     * Checks if the search string matches any parameter in the map.
     * If search contains "=", treats it as key=value and checks for exact match.
     */
    private static boolean matchesParameters(String search, Map<String, Object> parameters) {
        if (search.contains("=")) {
            String[] parts = search.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                Object paramValue = parameters.get(key);
                return paramValue != null && paramValue.toString().equalsIgnoreCase(value);
            }
        }
        return false;
    }
}

package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

/**
 * Utility class for searching jobs by name, type, parameters, and metadata.
 */
@ApplicationScoped
public class JobSearchUtils {

    private final ResolveParametersUseCase resolveParametersUseCase;

    @Inject
    public JobSearchUtils(ResolveParametersUseCase resolveParametersUseCase) {
        this.resolveParametersUseCase = resolveParametersUseCase;
    }

    /**
     * Applies search filter to a list of job executions.
     * Searches in job name, job type, parameters (if search contains "="), and metadata (if search contains "=").
     */
    public List<JobExecutionInfo> applySearchToExecutions(String search, List<JobExecutionInfo> executions) {
        if (search == null || search.isBlank()) {
            return executions;
        }

        final String searchLower = search.toLowerCase();
        return executions.stream()
                .filter(e -> e.getJobName().toLowerCase().contains(searchLower) ||
                        e.getJobType().toLowerCase().contains(searchLower) ||
                        matchesParameters(search, e.getParameters()) ||
                        matchesMetadata(search, e.getMetadata()))
                .toList();
    }

    /**
     * Applies search filter to a list of scheduled jobs.
     * Searches in job name, job type, and parameters (if search contains "=").
     */
    public List<ScheduledJobInfo> applySearchToScheduledJobs(String search, List<ScheduledJobInfo> jobs) {
        if (search == null || search.isBlank()) {
            return jobs;
        }

        final String searchLower = search.toLowerCase();
        return jobs.stream()
                .filter(j -> j.jobName().toLowerCase().contains(searchLower) ||
                        j.jobDefinition().jobType().toLowerCase().contains(searchLower) ||
                        matchesParameters(search, resolveParametersUseCase.execute(j)))
                .toList();
    }

    /**
     * Checks if the search string matches any key-value pair in the map.
     * If search contains "=", treats it as key=value and checks for exact match.
     */
    private boolean matchesKeyValue(String search, Map<String, Object> map) {
        if (search.contains("=")) {
            String[] parts = search.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().toLowerCase();
                Object mapValue = map.get(key);
                return mapValue != null && mapValue.toString().toLowerCase().contains(value);
            }
        }
        return false;
    }

    /**
     * Checks if the search string matches any parameter in the map.
     * If search contains "=", treats it as key=value and checks for exact match.
     */
    private boolean matchesParameters(String search, Map<String, Object> parameters) {
        return matchesKeyValue(search, parameters);
    }

    /**
     * Checks if the search string matches any metadata in the map.
     * If search contains "=", treats it as key=value and checks for exact match.
     */
    private boolean matchesMetadata(String search, Map<String, Object> metadata) {
        return matchesKeyValue(search, metadata);
    }
}

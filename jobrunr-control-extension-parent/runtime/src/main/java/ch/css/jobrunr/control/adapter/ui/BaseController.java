package ch.css.jobrunr.control.adapter.ui;

import ch.css.jobrunr.control.application.discovery.DiscoverJobsUseCase;
import ch.css.jobrunr.control.application.parameters.ResolveParametersUseCase;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.domain.ScheduledJobInfoView;
import io.quarkus.qute.TemplateInstance;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;

/**
 * Base controller providing common HTMX utilities and helper methods.
 * Reduces duplicate code across controllers by centralizing common patterns.
 */
public abstract class BaseController {

    private static final Logger LOG = Logger.getLogger(BaseController.class);

    /**
     * Builds a response that returns the updated content.
     * The modal will be closed automatically via htmx:afterSwap event listener.
     *
     * @param content the template instance to return
     * @return response with the updated content
     */
    protected Response buildModalCloseResponse(TemplateInstance content) {
        return Response.ok(content).build();
    }

    /**
     * Builds an error response that displays in the modal's alert area.
     * The modal stays open so the user can fix the error.
     * Uses HTMX out-of-band (OOB) swap to reliably target the alert container.
     *
     * @param errorMessage the error message to display
     * @return response with error alert HTML
     */
    protected Response buildErrorResponse(String errorMessage) {
        String errorHtml = String.format(
                "<div id=\"form-alerts\" hx-swap-oob=\"true\">" +
                        "<div class=\"alert alert-danger alert-dismissible fade show\" role=\"alert\">" +
                        "<i class=\"bi bi-exclamation-triangle-fill\"></i> <strong>Error:</strong> %s" +
                        "<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"alert\" aria-label=\"Close\"></button>" +
                        "</div>" +
                        "</div>",
                errorMessage
        );
        return Response.ok(errorHtml)
                .header("HX-Trigger", "scrollToError")
                .build();
    }

    /**
     * Parses a scheduled time string into an Instant.
     * Converts LocalDateTime from form input to Instant using system default timezone.
     *
     * @param scheduledAt the datetime string from the form (ISO format)
     * @return the parsed Instant, or null if input is blank
     */
    protected Instant parseScheduledTime(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isBlank()) {
            return null;
        }
        LocalDateTime ldt = LocalDateTime.parse(scheduledAt);
        return ldt.atZone(ZoneId.systemDefault()).toInstant();
    }

    /**
     * Extracts parameter map from form data.
     * Handles both single-value and multi-value parameters (e.g., from multiselect).
     * Multi-value parameters are joined with commas.
     *
     * @param allFormParams the form parameters from the request
     * @return map of parameter names to values
     */
    protected Map<String, String> extractParameterMap(MultivaluedMap<String, String> allFormParams) {
        return allFormParams.keySet().stream()
                .collect(HashMap::new,
                        (map, key) -> {
                            List<String> values = allFormParams.get(key);
                            if (values != null && values.size() > 1) {
                                // Multiple values (e.g., from multiselect) - join with comma
                                map.put(key, String.join(",", values));
                            } else {
                                // Single value
                                map.put(key, allFormParams.getFirst(key));
                            }
                        },
                        HashMap::putAll);
    }

    /**
     * Truncates large parameter values to prevent HTTP 413 errors.
     * String values longer than maxStringLength are truncated.
     * Collection/Map sizes are limited to prevent excessive data transfer.
     *
     * @param parameters the parameters to truncate
     * @return truncated parameter map
     */
    protected Map<String, Object> truncateParameterValues(Map<String, Object> parameters) {
        Map<String, Object> truncated = new HashMap<>();

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            Object truncatedValue = truncateValue(entry.getValue());
            truncated.put(entry.getKey(), truncatedValue);
        }

        return truncated;
    }

    private Object truncateValue(Object value) {
        return switch (value) {
            case String str -> truncateString(str);
            case Collection<?> collection -> truncateCollection(collection);
            case Map<?, ?> map -> truncateMap(map);
            default -> value;
        };
    }

    private Object truncateString(String str) {
        final int maxStringLength = 1000;

        if (str.length() > maxStringLength) {
            return str.substring(0, maxStringLength) + "... [truncated]";
        }
        return str;
    }

    private Object truncateCollection(Collection<?> collection) {
        final int maxCollectionSize = 100;

        if (collection.size() > maxCollectionSize) {
            return String.format("[Collection with %d items - too large to display]", collection.size());
        }
        return collection;
    }

    private Object truncateMap(Map<?, ?> map) {
        final int maxCollectionSize = 100;

        if (map.size() > maxCollectionSize) {
            return String.format("[Map with %d entries - too large to display]", map.size());
        }
        return map;
    }

    /**
     * Converts ScheduledJobInfo to ScheduledJobInfoView with resolved parameters.
     * If the job uses external parameter storage, the parameters are loaded from the parameter set.
     * Parameters are truncated to avoid sending large data in the list view.
     *
     * @param jobInfo                  the job info to convert
     * @param resolveParametersUseCase the use case to resolve external parameters
     * @return view model with resolved and truncated parameters
     */
    protected ScheduledJobInfoView toView(ScheduledJobInfo jobInfo, ResolveParametersUseCase resolveParametersUseCase) {
        boolean usesExternal = jobInfo.hasExternalParameters();
        Map<String, Object> resolvedParameters = resolveParametersUseCase.execute(jobInfo);

        // Truncate large parameter values to prevent 413 (Request Entity Too Large) errors
        Map<String, Object> truncatedParameters = truncateParameterValues(resolvedParameters);

        return ScheduledJobInfoView.from(jobInfo, truncatedParameters, usesExternal);
    }

    /**
     * Filters, searches, sorts and paginates a list of scheduled jobs.
     * Common logic shared between ScheduledJobsController and TemplatesController.
     *
     * @param jobs               the list of jobs to process
     * @param jobType            optional job type filter
     * @param search             optional search query
     * @param sortBy             field to sort by
     * @param sortOrder          sort order (asc/desc)
     * @param page               page number
     * @param size               page size
     * @param comparatorSupplier function to get comparator for sorting
     * @return pagination result with filtered, sorted and paginated jobs
     */
    @SuppressWarnings("java:S107")
    protected PaginationHelper.PaginationResult<ScheduledJobInfo> filterSortAndPaginate(
            List<ScheduledJobInfo> jobs,
            String jobType,
            String search,
            String sortBy,
            String sortOrder,
            int page,
            int size,
            Function<String, Comparator<ScheduledJobInfo>> comparatorSupplier) {

        // Apply job type filter
        List<ScheduledJobInfo> filteredJobs = jobs;
        if (jobType != null && !jobType.isBlank() && !"all".equals(jobType)) {
            filteredJobs = jobs.stream()
                    .filter(job -> jobType.equals(job.getJobType()))
                    .toList();
        }

        // Apply search
        List<ScheduledJobInfo> searchedJobs = JobSearchUtils.applySearchToScheduledJobs(search, filteredJobs);

        // Apply sorting
        Comparator<ScheduledJobInfo> comparator = comparatorSupplier.apply(sortBy);
        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }
        List<ScheduledJobInfo> sortedJobs = searchedJobs.stream()
                .sorted(comparator)
                .toList();

        // Apply pagination
        return PaginationHelper.paginate(sortedJobs, page, size);
    }

    /**
     * Result of resolving parameters and loading parameter definitions.
     * Used to avoid code duplication in edit modals.
     */
    protected static class ResolvedJobData {
        public final ScheduledJobInfo jobInfoWithResolvedParams;
        public final List<JobParameter> parameters;

        public ResolvedJobData(ScheduledJobInfo jobInfoWithResolvedParams, List<JobParameter> parameters) {
            this.jobInfoWithResolvedParams = jobInfoWithResolvedParams;
            this.parameters = parameters;
        }
    }

    /**
     * Resolves parameters and loads parameter definitions for a job.
     * Common logic shared between edit modals.
     *
     * @param jobInfo           the job info
     * @param resolveParameters function to resolve parameters
     * @param getJobParameters  function to get job parameter definitions
     * @return resolved job data
     */
    protected ResolvedJobData resolveJobParameters(
            ScheduledJobInfo jobInfo,
            Function<ScheduledJobInfo, Map<String, Object>> resolveParameters,
            Function<String, List<JobParameter>> getJobParameters) {

        // Resolve parameters (load external parameter sets if applicable)
        Map<String, Object> resolvedParameters = resolveParameters.apply(jobInfo);

        // Create a new ScheduledJobInfo with resolved parameters for the form
        ScheduledJobInfo jobInfoWithResolvedParams = new ScheduledJobInfo(
                jobInfo.getJobId(),
                jobInfo.getJobName(),
                jobInfo.getJobDefinition(),
                jobInfo.getScheduledAt(),
                resolvedParameters,
                jobInfo.isExternallyTriggerable(),
                jobInfo.getLabels()
        );

        // Load parameter definitions for this job type
        List<JobParameter> parameters = Collections.emptyList();
        try {
            parameters = getJobParameters.apply(jobInfo.getJobType());
        } catch (Exception e) {
            LOG.errorf("Error loading parameters for job type '%s': %s",
                    jobInfo.getJobType(), e.getMessage(), e);
        }

        return new ResolvedJobData(jobInfoWithResolvedParams, parameters);
    }

    /**
     * Gets all job definitions sorted by job type.
     * Common helper used by multiple controllers.
     *
     * @param discoverJobsUseCase the discovery use case
     * @return sorted list of job definitions
     */
    protected List<JobDefinition> getSortedJobDefinitions(DiscoverJobsUseCase discoverJobsUseCase) {
        return discoverJobsUseCase.execute().stream()
                .sorted(Comparator.comparing(JobDefinition::jobType))
                .toList();
    }

    /**
     * Gets all available job types sorted alphabetically.
     * Common helper used by multiple controllers.
     *
     * @param discoverJobsUseCase the discovery use case
     * @return sorted list of job type strings
     */
    protected List<String> getAvailableJobTypes(DiscoverJobsUseCase discoverJobsUseCase) {
        return discoverJobsUseCase.execute().stream()
                .map(JobDefinition::jobType)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }
}

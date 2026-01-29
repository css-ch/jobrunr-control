package ch.css.jobrunr.control.infrastructure.jobrunr.config;

import ch.css.jobrunr.control.infrastructure.jobrunr.filters.ParameterCleanupJobFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.jobrunr.jobs.filters.ApplyStateFilter;

/**
 * Configuration for JobRunr filters.
 * Registers custom filters with JobRunr.
 */
@ApplicationScoped
public class JobRunrFilterConfig {

    /**
     * Registers the parameter cleanup filter with JobRunr.
     * This filter automatically cleans up external parameter sets when jobs are deleted.
     */
    @Produces
    @ApplicationScoped
    public ApplyStateFilter parameterCleanupFilter(ParameterCleanupJobFilter filter) {
        return filter;
    }
}

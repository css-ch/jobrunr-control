package ch.css.jobrunr.control.application.discovery;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;

/**
 * Use Case: Loads all available job definitions.
 * Uses caching with 15 minutes TTL.
 */
@ApplicationScoped
public class DiscoverJobsUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public DiscoverJobsUseCase(JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    /**
     * Returns all available job definitions (with caching).
     *
     * @return List of all job definitions
     */
    public Collection<JobDefinition> execute() {
        return jobDefinitionDiscoveryService.getAllJobDefinitions();
    }
}


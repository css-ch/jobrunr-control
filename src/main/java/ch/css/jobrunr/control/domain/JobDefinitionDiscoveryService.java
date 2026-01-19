package ch.css.jobrunr.control.domain;

import java.util.List;
import java.util.Optional;

/**
 * Service interface for job discovery.
 * Enables finding and listing available job definitions.
 */
public interface JobDefinitionDiscoveryService {

    /**
     * Returns all available job definitions.
     *
     * @return List of all job definitions
     */
    List<JobDefinition> getAllJobDefinitions();

    /**
     * Finds a job definition by type.
     *
     * @param type Job type
     * @return Optional with the job definition, if found
     */
    Optional<JobDefinition> findJobByType(String type);

}


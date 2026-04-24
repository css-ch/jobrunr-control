package ch.css.jobrunr.control.domain;

import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;

import java.util.Collection;
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
    Collection<JobDefinition> getAllJobDefinitions();

    /**
     * Finds a job definition by type.
     *
     * @param type Job type
     * @return Optional with the job definition, if found
     */
    Optional<JobDefinition> findJobByType(String type);

    /**
     * Finds a job definition by the fully qualified handler class name. Use this when resolving
     * a JobDefinition from data originating in JobRunr itself (e.g. {@code JobDetails.getClassName()}),
     * where the handler FQN is authoritative and the simple class name may diverge from the
     * {@link JobDefinition#jobType()} whenever {@code @ConfigurableJob(jobType = "...")} is set.
     *
     * @param handlerClassName Fully qualified name of the {@code JobRequestHandler} implementation
     * @return Optional with the job definition, if found
     */
    Optional<JobDefinition> findJobByHandlerClassName(String handlerClassName);

    /**
     * Returns the job definition for the given type, or throws if not found.
     *
     * @param type Job type
     * @return The job definition
     * @throws JobNotFoundException if no definition exists for the given type
     */
    default JobDefinition requireJobByType(String type) {
        return findJobByType(type)
                .orElseThrow(() -> new JobNotFoundException("Job type '" + type + "' not found"));
    }
}

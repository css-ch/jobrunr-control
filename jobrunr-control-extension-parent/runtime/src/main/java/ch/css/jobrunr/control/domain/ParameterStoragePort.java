package ch.css.jobrunr.control.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for storing and retrieving parameter sets.
 * Infrastructure adapters implement this interface to provide
 * different storage mechanisms (database, cache, etc.).
 */
public interface ParameterStoragePort {

    /**
     * Stores a parameter set.
     *
     * @param parameterSet the parameter set to store
     */
    void store(ParameterSet parameterSet);

    /**
     * Retrieves a parameter set by ID.
     *
     * @param id the parameter set ID
     * @return the parameter set if found
     */
    Optional<ParameterSet> findById(UUID id);

    /**
     * Updates an existing parameter set.
     * If the parameter set does not exist, it will be created.
     *
     * @param parameterSet the parameter set to update
     */
    void update(ParameterSet parameterSet);

    /**
     * Deletes a parameter set by ID.
     *
     * @param id the parameter set ID
     */
    void deleteById(UUID id);
}

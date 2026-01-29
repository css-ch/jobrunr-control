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
     * Deletes a parameter set by ID.
     *
     * @param id the parameter set ID
     */
    void deleteById(UUID id);


    /**
     * Updates last accessed timestamp for a parameter set.
     *
     * @param id the parameter set ID
     */
    void updateLastAccessed(UUID id);
}

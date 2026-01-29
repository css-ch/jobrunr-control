package ch.css.jobrunr.control.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for checking and managing external parameter storage availability.
 * <p>
 * This port allows use cases to check if external parameter storage is configured
 * and to perform storage operations without depending on infrastructure details.
 */
public interface ParameterStorageService {

    /**
     * Checks if external parameter storage is available.
     *
     * @return true if external storage is configured and available
     */
    boolean isExternalStorageAvailable();

    /**
     * Stores a parameter set using external storage.
     *
     * @param parameterSet the parameter set to store
     * @throws IllegalStateException if external storage not configured
     */
    void store(ParameterSet parameterSet);

    /**
     * Finds a parameter set by ID.
     *
     * @param id the parameter set ID
     * @return optional containing the parameter set if found
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

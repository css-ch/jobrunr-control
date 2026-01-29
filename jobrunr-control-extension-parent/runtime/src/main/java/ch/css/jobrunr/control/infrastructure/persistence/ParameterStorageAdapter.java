package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * Infrastructure adapter for parameter storage operations.
 * Implements the domain port by checking availability and delegating to storage adapters.
 * <p>
 * This adapter checks if JpaParameterStorageAdapter is available (Hibernate ORM enabled)
 * and delegates operations to it. If not available, operations fail gracefully.
 */
@ApplicationScoped
public class ParameterStorageAdapter implements ParameterStorageService {

    private static final Logger log = Logger.getLogger(ParameterStorageAdapter.class);

    private final Instance<ParameterStoragePort> storageAdapters;

    @Inject
    public ParameterStorageAdapter(@Any Instance<ParameterStoragePort> storageAdapters) {
        this.storageAdapters = storageAdapters;
    }

    @Override
    public boolean isExternalStorageAvailable() {
        try {
            return storageAdapters.select(JpaParameterStorageAdapter.class).isResolvable();
        } catch (Exception e) {
            log.debugf("External storage check failed: %s", e.getMessage());
            return false;
        }
    }

    /**
     * Stores a parameter set using external storage.
     *
     * @param parameterSet the parameter set to store
     * @throws IllegalStateException if external storage not configured
     */
    @Override
    public void store(ParameterSet parameterSet) {
        if (!isExternalStorageAvailable()) {
            throw new IllegalStateException(
                    "External parameter storage not available. " +
                            "Ensure Hibernate ORM is enabled (quarkus.hibernate-orm.enabled=true)");
        }
        getExternalStorage().store(parameterSet);
        log.debugf("Stored parameter set: %s", parameterSet.id());
    }

    /**
     * Finds a parameter set by ID.
     *
     * @param id the parameter set ID
     * @return optional containing the parameter set if found
     */
    @Override
    public Optional<ParameterSet> findById(UUID id) {
        if (!isExternalStorageAvailable()) {
            log.warnf("External storage not available, cannot load parameter set: %s", id);
            return Optional.empty();
        }
        return getExternalStorage().findById(id);
    }

    /**
     * Deletes a parameter set by ID.
     *
     * @param id the parameter set ID
     */
    @Override
    public void deleteById(UUID id) {
        if (isExternalStorageAvailable()) {
            getExternalStorage().deleteById(id);
            log.debugf("Deleted parameter set: %s", id);
        }
    }

    /**
     * Updates last accessed timestamp for a parameter set.
     *
     * @param id the parameter set ID
     */
    public void updateLastAccessed(UUID id) {
        if (isExternalStorageAvailable()) {
            getExternalStorage().updateLastAccessed(id);
        }
    }

    /**
     * Gets the external storage adapter.
     *
     * @return the JPA parameter storage adapter
     * @throws IllegalStateException if not available
     */
    private ParameterStoragePort getExternalStorage() {
        return storageAdapters.select(JpaParameterStorageAdapter.class).get();
    }
}

package ch.css.jobrunr.control.infrastructure.persistence;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

/**
 * No-op implementation for inline parameter storage (default).
 * When this adapter is active, parameters are stored directly in JobRunr's job table.
 */
@ApplicationScoped
@DefaultBean
public class InlineParameterStorageAdapter implements ParameterStoragePort {

    @Override
    public void store(ParameterSet parameterSet) {
        // No-op: parameters stored inline in job
    }

    @Override
    public Optional<ParameterSet> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public void deleteById(UUID id) {
        // No-op
    }


    @Override
    public void updateLastAccessed(UUID id) {
        // No-op
    }
}

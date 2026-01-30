package ch.css.jobrunr.control.domain.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a referenced parameter set cannot be found.
 */
public class ParameterSetNotFoundException extends RuntimeException {

    private final UUID parameterSetId;

    public ParameterSetNotFoundException(UUID parameterSetId) {
        super("Parameter set not found: " + parameterSetId);
        this.parameterSetId = parameterSetId;
    }

    public UUID getParameterSetId() {
        return parameterSetId;
    }
}

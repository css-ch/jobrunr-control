package ch.css.jobrunr.control.domain.exceptions;

/**
 * Exception thrown when parameter serialization/deserialization fails.
 * This is a runtime exception that indicates a technical problem with parameter handling.
 */
public class ParameterSerializationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ParameterSerializationException(String message) {
        super(message);
    }

    public ParameterSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

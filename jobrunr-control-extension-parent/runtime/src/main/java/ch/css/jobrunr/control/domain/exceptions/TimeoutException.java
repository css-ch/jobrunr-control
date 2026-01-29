package ch.css.jobrunr.control.domain.exceptions;

/**
 * Exception thrown when an operation times out.
 */
public class TimeoutException extends RuntimeException {

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

package ch.css.jobrunr.control.domain.exceptions;

/**
 * Exception thrown when an error occurs during job scheduling operations.
 */
public class JobSchedulingException extends RuntimeException {

    public JobSchedulingException(String message) {
        super(message);
    }

    public JobSchedulingException(String message, Throwable cause) {
        super(message, cause);
    }
}

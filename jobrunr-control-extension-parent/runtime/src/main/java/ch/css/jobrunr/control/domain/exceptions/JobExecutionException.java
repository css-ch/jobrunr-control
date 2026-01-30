package ch.css.jobrunr.control.domain.exceptions;

/**
 * Exception thrown when an error occurs during job execution operations.
 */
public class JobExecutionException extends RuntimeException {

    public JobExecutionException(String message) {
        super(message);
    }

    public JobExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}

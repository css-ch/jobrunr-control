package ch.css.jobrunr.control.domain.exceptions;

/**
 * Exception thrown when an error occurs during the actual processing/execution of a job's business logic.
 * This exception represents failures in the job's run() method, such as data processing errors,
 * external service failures, or business rule violations.
 */
public class JobProcessingException extends RuntimeException {

    public JobProcessingException(String message) {
        super(message);
    }

    public JobProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public JobProcessingException(Throwable cause) {
        super(cause);
    }
}

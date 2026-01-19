package ch.css.jobrunr.control.application.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception for validation errors in job parameters.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = Collections.singletonList(message);
    }

    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
        this.errors = new ArrayList<>(errors);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}


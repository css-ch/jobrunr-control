package ch.css.jobrunr.control.domain.exceptions;

/**
 * Exception thrown when a template with the same name already exists.
 */
public class DuplicateTemplateNameException extends RuntimeException {

    public DuplicateTemplateNameException(String name) {
        super("A template with name '" + name + "' already exists");
    }
}

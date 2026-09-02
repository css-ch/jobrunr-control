package ch.css.jobrunr.control.annotations;

/**
 * Precision of the {@code datetime-local} UI input for {@code DATETIME} parameters.
 * <p>
 * Controls how fine-grained the value the user can pick/enter is. Defaults to
 * {@link #MINUTES} to keep the UI simple for the common case; {@link #SECONDS} and
 * {@link #MILLISECONDS} are opt-in for jobs that require finer-grained scheduling
 * or trigger times.
 */
public enum DateTimePrecision {
    MINUTES("60"),
    SECONDS("1"),
    MILLISECONDS("0.001");

    private final String stepAttribute;

    DateTimePrecision(String stepAttribute) {
        this.stepAttribute = stepAttribute;
    }

    /**
     * Returns the value to use for the HTML {@code step} attribute of an
     * {@code <input type="datetime-local">} element to achieve this precision.
     */
    public String stepAttribute() {
        return stepAttribute;
    }
}

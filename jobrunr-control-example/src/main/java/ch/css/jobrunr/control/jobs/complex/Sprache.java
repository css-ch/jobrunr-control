package ch.css.jobrunr.control.jobs.complex;

public enum Sprache {
    DEUTSCH("de", "Deutsch"),
    ENGLISH("en", "English"),
    FRENCH("fr", "Französisch"),
    ITALIAN("it", "Italienisch");

    private final String value;
    private final String label;

    Sprache(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}

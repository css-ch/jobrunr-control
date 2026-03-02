package ch.css.jobrunr.control.jobs.complex;

public enum WunschVersandArt {
    STANDARD("Standardversand"),
    EXPRESS("Expressversand"),
    ABHOLUNG("Abholung im Laden");

    private final String label;

    WunschVersandArt(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

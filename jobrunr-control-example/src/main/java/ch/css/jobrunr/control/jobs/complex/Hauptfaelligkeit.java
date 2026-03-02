package ch.css.jobrunr.control.jobs.complex;

public enum Hauptfaelligkeit {
    JANUARY("01.01.", "Januar"),
    FEBRUARY("01.02.", "Februar"),
    MARCH("01.03.", "März"),
    APRIL("01.04.", "April"),
    MAY("01.05.", "Mai"),
    JUNE("01.06.", "Juni"),
    JULY("01.07.", "Juli"),
    AUGUST("01.08.", "August"),
    SEPTEMBER("01.09.", "September"),
    OCTOBER("01.10.", "Oktober"),
    NOVEMBER("01.11.", "November"),
    DECEMBER("01.12.", "Dezember");

    private final String value;
    private final String label;

    Hauptfaelligkeit(String value, String label) {
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

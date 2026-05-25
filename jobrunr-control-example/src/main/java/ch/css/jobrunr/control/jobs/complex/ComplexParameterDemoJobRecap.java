package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.JobRecapParameter;

public record ComplexParameterDemoJobRecap(
        @JobRecapParameter(
                displayName = "Policen selektiert",
                description = "Anzahl von allen selektieren Policen",
                icon = "bi-check-circle",
                css = "color: red;",
                order = 1)
        long policenSelektiert,             // Policen selektiert
        @JobRecapParameter(
                displayName = "Policen für die alle relevanten Policenschnitte gedruckt wurden",
                description = "Anzahl der Policen, für welche die relevanten Policenschnitte ein Druckstück erzeugt worden ist.",
                order = 2)
        long policenRelevant,               // Policen für die alle relevanten Policenschnitte gedruckt wurden
        @JobRecapParameter(
                displayName = "Policen mit fachlichem Fehler",
                description = "Policen, welche mit fachlichem Fehler verarbeitet wurden.",
                order = 3)
        long policenFailed,                 // Policen mit fachlichem Fehler
        @JobRecapParameter(
                displayName = "Policen mit Policensperre",
                description = "Policen, welche eine Policen-Sperre gesetzt haben und deshalb nicht verarbeitet wurden.",
                order = 4)
        long policenSperre,                 // Policen mit Policensperre
        @JobRecapParameter(
                displayName = "Policen annuliert",
                description = "Policen, welche seit dem Druckauftrag annulliert worden sind und deshalb der Druckauftrag nicht mehr verarbeitet wird",
                order = 5)
        long policenAnnulliert,              // Policen annuliert
        @JobRecapParameter(displayName = "Policen herausgefiltert via Template", order = 6)
        long policenHerausgefilter,         // Police herausgefiltert via Template
        @JobRecapParameter(displayName = "Druckaufträge: verarbeitete Policen-Schnitte für die selektierten Policen", order = 7)
        long druckauftraegeVerarbeitet,     // Druckaufträge: verarbeitete Policen-Schnitte für die selektierten Policen
        @JobRecapParameter(displayName = "Druckaufträge: gedruckte Policen-Schnitte für die selektierten Policen", order = 8)
        long druckauftraegeGedruckt         // Druckaufträge: gedruckte Policen-Schnitte für die selektierten Policen
) {

    /**
     * Creates a new Builder for fluent construction of ComplexParameterDemoJobRecap.
     * All fields default to 0.
     *
     * Example:
     * <pre>
     * ComplexParameterDemoJobRecap recap = ComplexParameterDemoJobRecap.builder()
     *     .policenSelektiert(100)
     *     .policenSperre(5)
     *     .build();
     * </pre>
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ComplexParameterDemoJobRecap with all fields defaulting to 0.
     */
    public static class Builder {
        private long policenSelektiert = 0;
        private long policenRelevant = 0;
        private long policenFailed = 0;
        private long policenSperre = 0;
        private long policenAnnuliert = 0;
        private long policenHerausgefilter = 0;
        private long druckauftraegeVerarbeitet = 0;
        private long druckauftraegeGedruckt = 0;

        public Builder policenSelektiert(long value) {
            this.policenSelektiert = value;
            return this;
        }

        public Builder policenRelevant(long value) {
            this.policenRelevant = value;
            return this;
        }

        public Builder policenFailed(long value) {
            this.policenFailed = value;
            return this;
        }

        public Builder policenSperre(long value) {
            this.policenSperre = value;
            return this;
        }

        public Builder policenAnnulliert(long value) {
            this.policenAnnuliert = value;
            return this;
        }

        public Builder policenHerausgefilter(long value) {
            this.policenHerausgefilter = value;
            return this;
        }

        public Builder druckauftraegeVerarbeitet(long value) {
            this.druckauftraegeVerarbeitet = value;
            return this;
        }

        public Builder druckauftraegeGedruckt(long value) {
            this.druckauftraegeGedruckt = value;
            return this;
        }

        /**
         * Builds the ComplexParameterDemoJobRecap instance with all configured values.
         */
        public ComplexParameterDemoJobRecap build() {
            return new ComplexParameterDemoJobRecap(
                    policenSelektiert,
                    policenRelevant,
                    policenFailed,
                    policenSperre,
                    policenAnnuliert,
                    policenHerausgefilter,
                    druckauftraegeVerarbeitet,
                    druckauftraegeGedruckt
            );
        }
    }
}

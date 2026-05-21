package ch.css.jobrunr.control.jobs.complex;

public record ComplexParameterDemoJobRecap(
        long policenSelektiert,             // Policen selektiert
        long policenRelevant,               // Policen für die alle relevanten Policenschnitte gedruckt wurden
        long policenFailed,                 // Policen mit fachlichem Fehler
        long policenSperre,                 // Policen mit Policensperre
        long policenAnnulliert,              // Policen annuliert
        long policenHerausgefilter,         // Police herausgefiltert via Template
        long druckauftraegeVerarbeitet,     // Druckaufträge: verarbeitete Policen-Schnitte für die selektierten Policen
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

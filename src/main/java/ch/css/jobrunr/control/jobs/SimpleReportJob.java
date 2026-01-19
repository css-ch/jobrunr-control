package ch.css.jobrunr.control.jobs;

import ch.css.jobrunr.control.infrastructure.discovery.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.context.JobDashboardLogger;

/**
 * Einfacher Demo-Job ohne Parameter.
 * Demonstriert die grundlegende Job-Funktionalität.
 */
@ApplicationScoped
public class SimpleReportJob implements ConfigurableJob<SimpleReportJobRequest> {

    @Override
    public void run(SimpleReportJobRequest request) throws Exception {
        JobDashboardLogger log = jobContext().logger();

        log.info("=== SimpleReportJob gestartet ===");

        try {
            // Simuliere Report-Generierung
            log.info("Sammle Daten...");
            Thread.sleep(2000);

            log.info("Generiere Report...");
            Thread.sleep(3000);

            log.info("Report erfolgreich erstellt!");
            log.info("Dateiname: report_" + System.currentTimeMillis() + ".pdf");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Job wurde unterbrochen: " + e);
            throw new RuntimeException("Job-Ausführung fehlgeschlagen", e);
        }

        log.info("=== SimpleReportJob abgeschlossen ===");
    }
}


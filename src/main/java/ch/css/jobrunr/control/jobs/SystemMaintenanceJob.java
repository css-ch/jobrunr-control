package ch.css.jobrunr.control.jobs;

import ch.css.jobrunr.control.infrastructure.discovery.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.context.JobDashboardLogger;

/**
 * Externally triggerable job for system maintenance.
 * Typically triggered manually or via API.
 */
@ApplicationScoped

public class SystemMaintenanceJob implements ConfigurableJob<SystemMaintenanceJobRequest> {

    @Override
    public void run(SystemMaintenanceJobRequest request) throws Exception {
        JobDashboardLogger log = jobContext().logger();

        log.info("=== SystemMaintenanceJob gestartet ===");
        log.info("Konfiguration:");
        log.info("  - Cache leeren: " + request.clearCache());
        log.info("  - Datenbank komprimieren: " + request.compactDatabase());
        log.info("  - Logs bereinigen: " + request.cleanupLogs());

        try {
            int tasksCompleted = 0;

            if (request.clearCache()) {
                log.info(">>> Task 1: Cache leeren");
                log.info("Analysiere Cache...");
                Thread.sleep(1000);
                log.info("Lösche veraltete Einträge...");
                Thread.sleep(2000);
                log.info("✓ Cache geleert: 1.234 Einträge entfernt");
                tasksCompleted++;
            }

            if (request.compactDatabase()) {
                log.info(">>> Task 2: Datenbank komprimieren");
                log.info("Analysiere Datenbank-Fragmentierung...");
                Thread.sleep(1500);
                log.info("Komprimiere Tabellen...");
                Thread.sleep(3000);
                log.info("Optimiere Indizes...");
                Thread.sleep(2000);
                log.info("✓ Datenbank komprimiert: 45 MB freigegeben");
                tasksCompleted++;
            }

            if (request.cleanupLogs()) {
                log.info(">>> Task 3: Logs bereinigen");
                log.info("Suche alte Log-Dateien...");
                Thread.sleep(1000);
                log.info("Archiviere Logs älter als 90 Tage...");
                Thread.sleep(2000);
                log.info("Lösche Logs älter als 1 Jahr...");
                Thread.sleep(1500);
                log.info("✓ Logs bereinigt: 156 Dateien archiviert, 23 gelöscht");
                tasksCompleted++;
            }

            if (tasksCompleted == 0) {
                log.warn("Keine Wartungsaufgaben ausgewählt!");
            } else {
                log.info("=== Zusammenfassung ===");
                log.info("{} Wartungsaufgaben erfolgreich abgeschlossen: " + tasksCompleted);
                log.info("System-Status: Optimal");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Wartung wurde unterbrochen: " + e);
            throw new RuntimeException("System-Wartung fehlgeschlagen", e);
        }

        log.info("=== SystemMaintenanceJob abgeschlossen ===");
    }
}

package ch.css.jobrunr.control.jobs;

import ch.css.jobrunr.control.infrastructure.discovery.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.context.JobDashboardLogger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Job for sending notifications.
 * Demonstrates DateTime parameters and conditional logic.
 */
@ApplicationScoped
public class NotificationJob implements ConfigurableJob<NotificationJobRequest> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Override
    public void run(NotificationJobRequest request) throws Exception {
        JobDashboardLogger log = jobContext().logger();

        log.info("=== NotificationJob started ===");
        log.info("Parameters:");
        log.info("  - recipientEmail: " + request.recipientEmail());
        log.info("  - subject: " + request.subject());
        log.info("  - sendImmediately: " + request.sendImmediately());
        log.info("  - scheduledTime: " + (request.scheduledTime() != null ? request.scheduledTime().format(FORMATTER) : "N/A"));

        try {
            if (request.sendImmediately()) {
                log.info("Immediate sending activated");
                sendNotification(log, request.recipientEmail(), request.subject());
            } else {
                log.info("Scheduled sending");
                log.info("Scheduled for: " + request.scheduledTime().format(FORMATTER));

                // Check if the time has been reached (Simulation)
                if (request.scheduledTime().isBefore(LocalDateTime.now())) {
                    log.info("Time reached - sending now");
                    sendNotification(log, request.recipientEmail(), request.subject());
                } else {
                    log.info("Time not yet reached - waiting...");
                    Thread.sleep(2000);
                    log.info("Simulation: Time reached");
                    sendNotification(log, request.recipientEmail(), request.subject());
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Job was interrupted: " + e);
            throw new RuntimeException("Notification failed", e);
        }

        log.info("=== NotificationJob completed ===");
    }

    private void sendNotification(JobDashboardLogger log, String recipient, String subject) throws InterruptedException {
        log.info("Creating message...");
        Thread.sleep(1000);

        log.info("Connecting to mail server...");
        Thread.sleep(1500);

        log.info("Sending email...");
        log.info("  To: " + recipient);
        log.info("  Subject: " + subject);
        Thread.sleep(1000);

        log.info("✓ Email successfully sent!");
        log.info("  Message-ID: msg-" + System.currentTimeMillis());
    }
}

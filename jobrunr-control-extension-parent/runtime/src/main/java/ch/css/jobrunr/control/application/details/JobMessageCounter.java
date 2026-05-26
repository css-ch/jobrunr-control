package ch.css.jobrunr.control.application.details;

public record JobMessageCounter(
        long totalMessages,
        long infoMessages,
        long warningMessages,
        long errorMessages) {
}

package ch.css.jobrunr.control.application.details;

public record JobMessageCounter(
        long infoMessages,
        long warningMessages,
        long errorMessages,
        long exceptionMessages) {

    public long totalMessages() {
        return infoMessages + warningMessages + errorMessages + exceptionMessages;
    }
}

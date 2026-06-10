package ch.css.jobrunr.control.domain.details;

public record JobMessageLevelCounters(
        long infoMessages,
        long warningMessages,
        long errorMessages,
        long exceptionMessages) {

    public long totalMessages() {
        return infoMessages + warningMessages + errorMessages + exceptionMessages;
    }
}

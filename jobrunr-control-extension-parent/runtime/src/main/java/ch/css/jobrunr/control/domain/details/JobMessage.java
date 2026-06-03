package ch.css.jobrunr.control.domain.details;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

public record JobMessage(
        Instant createdAt,
        UUID jobId,
        JobMessageLevel messageLevel,
        String message,
        String stackTrace) {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public String createdAtFormatted() {
        return createdAt.atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMATTER);
    }

    public boolean hasStackTrace() {
        return stackTrace != null && !stackTrace.isBlank();
    }

    public String stackTracePreview() {
        if (!hasStackTrace()) {
            return "";
        }
        return Arrays.stream(stackTrace.split("\\R"))
                .limit(3)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    public boolean isStackTraceTruncated() {
        if (!hasStackTrace()) {
            return false;
        }
        return Arrays.stream(stackTrace.split("\\R")).count() > 3;
    }
}

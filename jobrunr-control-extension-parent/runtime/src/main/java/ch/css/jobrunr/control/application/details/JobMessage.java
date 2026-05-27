package ch.css.jobrunr.control.application.details;

import java.time.Instant;
import java.util.Arrays;

public record JobMessage(
        Instant createdAt,
        JobMessageLevel messageLevel,
        String message,
        String formattedCreatedAt,
        String stackTrace) {

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

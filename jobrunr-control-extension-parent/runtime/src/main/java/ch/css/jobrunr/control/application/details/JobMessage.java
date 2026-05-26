package ch.css.jobrunr.control.application.details;

import java.time.Instant;

public record JobMessage(
        Instant createdAt,
        JobMessageLevel messageLevel,
        String message,
        String formattedCreatedAt) {
}

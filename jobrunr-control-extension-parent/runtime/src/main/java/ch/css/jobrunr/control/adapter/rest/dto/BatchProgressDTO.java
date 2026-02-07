package ch.css.jobrunr.control.adapter.rest.dto;

/**
 * DTO for batch progress information.
 *
 * @param total     Total number of items
 * @param succeeded Number of successful items
 * @param failed    Number of failed items
 * @param pending   Number of pending items
 * @param progress  Progress percentage (0.0 to 100.0)
 */
public record BatchProgressDTO(
        long total,
        long succeeded,
        long failed,
        long pending,
        double progress
) {
}

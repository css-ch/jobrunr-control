package ch.css.jobrunr.control.adapter.rest.dto;

/**
 * DTO for batch progress information.
 */
public record BatchProgressDTO(
        long total,
        long succeeded,
        long failed,
        long pending,
        double progress
) {
}

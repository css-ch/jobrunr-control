package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.Map;

/**
 * Request DTO for starting a job (regular or template).
 * When starting a template job, the postfix can be provided to customize the cloned job name.
 * When starting a regular job, the postfix is ignored.
 *
 * @param postfix    Optional postfix for template job names
 * @param parameters Optional parameter overrides
 */
public record StartJobRequestDTO(
        String postfix,
        Map<String, Object> parameters
) {
}

package ch.css.jobrunr.control.adapter.rest.dto;

import java.util.Map;

/**
 * Request DTO for starting a template job.
 */
public record StartTemplateRequestDTO(
        String postfix,
        Map<String, Object> parameters
) {
}

package ch.css.jobrunr.control.domain;

import java.util.List;

public record JobSettings(
        String name,
        boolean isBatch,
        int retries,
        List<String> labels,
        List<String> jobFilters,
        String queue,
        String runOnServerWithTag,
        String mutex,
        String rateLimiter,
        String processTimeOut,
        String deleteOnSuccess,
        String deleteOnFailure
) {
}

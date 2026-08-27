package ch.css.jobrunr.control.domain.details;

import java.util.List;

/**
 * Application-provided SPI for linking batch messages to external tools.
 *
 * <p>Implementations must be CDI beans. If no implementation is present,
 * the batch message detail page displays no external links.</p>
 */
public interface BatchMessageExternalLinkProvider {

    /**
     * Determines the order of this provider relative to other providers.
     * Lower values are rendered first.
     */
    default int order() {
        return 1000;
    }

    List<BatchMessageExternalLink> linksFor(JobMessage message);
}

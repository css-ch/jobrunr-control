package ch.css.jobrunr.control.domain.details;

import java.util.Map;
import java.util.UUID;

public interface JobRecapProvider {

    String providerKey();

    Map<String, Long> determineRecap(UUID jobId);

    /**
     * Deletes all recap values belonging to a logical workflow root.
     *
     * @param rootJobId canonical workflow root
     */
    default void deleteByRootJobId(UUID rootJobId) {
        // Optional for providers that do not persist recaps.
    }
}

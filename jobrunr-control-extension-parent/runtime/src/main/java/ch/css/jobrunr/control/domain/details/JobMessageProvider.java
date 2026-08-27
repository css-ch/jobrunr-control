package ch.css.jobrunr.control.domain.details;

import java.util.UUID;

public interface JobMessageProvider {

    String providerKey();

    JobMessagesPaged searchJobMessages(UUID jobId,
                                       JobMessageLevelSearch levelSearch,
                                       String textSearch,
                                       JobMessageSortOrder sortOrder,
                                       JobMessageAttemptFilter attemptFilter,
                                       int pageNumber,
                                       int pageSize);

    JobMessageLevelCounters determineJobMessageCounter(UUID jobId,
                                                       JobMessageAttemptFilter attemptFilter);

    /**
     * Deletes all messages belonging to a logical workflow root.
     *
     * @param rootJobId canonical workflow root
     */
    default void deleteByRootJobId(UUID rootJobId) {
        // Optional for providers that do not persist messages.
    }
}

package ch.css.jobrunr.control.domain.details;

import java.util.UUID;

public interface JobMessageStoragePort {

    void writeMessage(UUID jobId, JobMessage message);

    void invalidatePreviousAttemptMessages(UUID rootJobId, UUID childJobId, int currentAttemptNr);

    JobMessagesPaged searchMessages(UUID jobId,
                                    JobMessageLevelSearch levelSearch,
                                    String textSearch,
                                    JobMessageSortOrder sortOrder,
                                    JobMessageAttemptFilter attemptFilter,
                                    int pageNr,
                                    int pageSize);

    JobMessageLevelCounters determineMessageLevelCounters(UUID jobId, JobMessageAttemptFilter attemptFilter);

}

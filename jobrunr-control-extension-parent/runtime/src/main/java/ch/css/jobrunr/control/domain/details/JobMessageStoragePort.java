package ch.css.jobrunr.control.domain.details;

import java.util.UUID;

public interface JobMessageStoragePort {

    void writeMessage(UUID jobId, JobMessage message);

    JobMessagesPaged searchMessages(UUID jobId, JobMessageLevelSearch levelSearch, String textSearch, JobMessageSortOrder sortOrder, int pageNr, int pageSize);

    JobMessageLevelCounters determineMessageLevelCounters(UUID jobId);

}

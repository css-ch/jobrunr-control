package ch.css.jobrunr.control.domain.details;

import java.util.UUID;

public interface JobMessageProvider {

    String providerKey();

    JobMessagesPaged searchJobMessages(UUID jobId,
                                       JobMessageLevelSearch levelSearch,
                                       String textSearch,
                                       JobMessageSortOrder sortOrder,
                                       int pageNumber,
                                       int pageSize);

    JobMessageLevelCounters determineJobMessageCounter(UUID jobId);
}

package ch.css.jobrunr.control.application.details;

import java.util.List;
import java.util.UUID;

public interface JobMessageProvider {

    String providerKey();

    PagedJobMessages searchJobMessages(UUID jobId,
                                       JobMessageLevelSearch levelSearch,
                                       String textSearch,
                                       JobMessageSortOrder sortOrder,
                                       int pageNumber,
                                       int pageSize);

    JobMessageCounter determineJobMessageCounter(UUID jobId);

    record PagedJobMessages(List<JobMessage> items, long totalItems, int page, int size) {}
}

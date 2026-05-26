package ch.css.jobrunr.control.application.details;

import java.util.List;
import java.util.UUID;

public interface JobMessageProvider {

    String providerKey();

    PagedJobMessages searchJobMessages(UUID jobId, String jobType, JobMessageSearch searchFilter, int pageNumber, int pageSize);

    JobMessageCounter determineJobMessageCounter(UUID jobId, String jobType);

    record PagedJobMessages(List<JobMessage> items, long totalItems, int page, int size) {}
}

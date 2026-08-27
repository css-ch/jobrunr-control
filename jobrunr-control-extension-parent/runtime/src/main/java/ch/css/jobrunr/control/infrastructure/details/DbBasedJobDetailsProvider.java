package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.details.JobMessageLevelCounters;
import ch.css.jobrunr.control.domain.details.JobMessageLevelSearch;
import ch.css.jobrunr.control.domain.details.JobMessageAttemptFilter;
import ch.css.jobrunr.control.domain.details.JobMessageProvider;
import ch.css.jobrunr.control.domain.details.JobMessageSortOrder;
import ch.css.jobrunr.control.domain.details.JobMessageStoragePort;
import ch.css.jobrunr.control.domain.details.JobMessagesPaged;
import ch.css.jobrunr.control.domain.details.JobRecapProvider;
import ch.css.jobrunr.control.domain.details.JobRecapStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DbBasedJobDetailsProvider implements JobMessageProvider, JobRecapProvider {

    public static final String PROVIDER_KEY = "db-based-job-details-provider";

    private final JobMessageStoragePort jobMessageStoragePort;
    private final JobRecapStoragePort jobRecapStoragePort;

    @Inject
    public DbBasedJobDetailsProvider(JobMessageStoragePort jobMessageStoragePort, JobRecapStoragePort jobRecapStoragePort) {
        this.jobMessageStoragePort = jobMessageStoragePort;
        this.jobRecapStoragePort = jobRecapStoragePort;
    }

    @Override
    public String providerKey() {
        return PROVIDER_KEY;
    }

    @Override
    public JobMessagesPaged searchJobMessages(UUID jobId,
                                              JobMessageLevelSearch levelSearch,
                                              String textSearch,
                                              JobMessageSortOrder sortOrder,
                                              JobMessageAttemptFilter attemptFilter,
                                              int pageNumber,
                                              int pageSize) {
        return jobMessageStoragePort.searchMessages(
                jobId, levelSearch, textSearch, sortOrder, attemptFilter, pageNumber, pageSize);
    }

    @Override
    public JobMessageLevelCounters determineJobMessageCounter(UUID jobId,
                                                              JobMessageAttemptFilter attemptFilter) {
        return jobMessageStoragePort.determineMessageLevelCounters(jobId, attemptFilter);
    }

    @Override
    public Map<String, Long> determineRecap(UUID jobId) {
        return jobRecapStoragePort.readRecap(jobId);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void deleteByRootJobId(UUID rootJobId) {
        jobMessageStoragePort.deleteByRootJobId(rootJobId);
        jobRecapStoragePort.deleteByRootJobId(rootJobId);
    }
}

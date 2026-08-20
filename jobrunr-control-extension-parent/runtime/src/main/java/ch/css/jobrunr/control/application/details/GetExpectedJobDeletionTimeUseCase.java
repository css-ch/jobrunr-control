package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.JobStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.storage.StorageProvider;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Determines when JobRunr has scheduled a completed job for deletion.
 */
@ApplicationScoped
public class GetExpectedJobDeletionTimeUseCase {

    private final JobExecutionPort jobExecutionPort;
    private final StorageProvider storageProvider;

    @Inject
    public GetExpectedJobDeletionTimeUseCase(JobExecutionPort jobExecutionPort,
                                             StorageProvider storageProvider) {
        this.jobExecutionPort = jobExecutionPort;
        this.storageProvider = storageProvider;
    }

    public Optional<Instant> execute(UUID jobId) {
        return jobExecutionPort.getJobChainExecutionById(jobId)
                .filter(this::isTerminalStatus)
                .map(jobExecutionInfo -> storageProvider.getJobById(jobId).getDeleteAt())
                .filter(java.util.Objects::nonNull);
    }

    private boolean isTerminalStatus(ch.css.jobrunr.control.domain.JobExecutionInfo jobExecutionInfo) {
        return jobExecutionInfo.getStatus() == JobStatus.SUCCEEDED
                || jobExecutionInfo.getStatus() == JobStatus.FAILED;
    }
}

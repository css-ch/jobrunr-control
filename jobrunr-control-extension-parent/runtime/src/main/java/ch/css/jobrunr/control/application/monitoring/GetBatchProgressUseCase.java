package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.domain.exceptions.TimeoutException;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Loads the batch progress for a job.
 * Counts subjobs asynchronously with timeout handling.
 */
@ApplicationScoped
public class GetBatchProgressUseCase {

    private final JobExecutionPort jobExecutionPort;
    private final Duration timeout;

    @Inject
    public GetBatchProgressUseCase(
            JobExecutionPort jobExecutionPort,
            @ConfigProperty(name = "jobrunr.batch-progress.timeout", defaultValue = "PT5S") Duration timeout) {
        this.jobExecutionPort = jobExecutionPort;
        this.timeout = timeout;
    }

    /**
     * Returns the batch progress for a job.
     *
     * @param jobId Job ID
     * @return Optional with batch progress, if available
     */
    public Optional<BatchProgress> execute(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        Optional<JobExecutionInfo> executionInfo = jobExecutionPort.getJobExecutionById(jobId);

        if (executionInfo.isEmpty()) {
            throw new JobNotFoundException("Job with ID '" + jobId + "' not found");
        }

        return executionInfo.get().getBatchProgress();
    }

    /**
     * Returns the batch progress asynchronously (with timeout).
     *
     * @param jobId Job ID
     * @return Uni with Optional of batch progress
     */
    public Uni<Optional<BatchProgress>> executeAsync(UUID jobId) {
        return Uni.createFrom()
                .item(() -> execute(jobId))
                .ifNoItem().after(timeout).failWith(
                        new TimeoutException("Timeout loading batch progress for job " + jobId)
                );
    }
}


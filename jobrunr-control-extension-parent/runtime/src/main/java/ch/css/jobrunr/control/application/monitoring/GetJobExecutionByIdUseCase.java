package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Returns a specific job execution by ID.
 */
@ApplicationScoped
public class GetJobExecutionByIdUseCase {

    private final JobExecutionPort jobExecutionPort;

    @Inject
    public GetJobExecutionByIdUseCase(JobExecutionPort jobExecutionPort) {
        this.jobExecutionPort = jobExecutionPort;
    }

    /**
     * Returns a job execution by ID.
     *
     * @param jobId Job ID
     * @return Job execution information
     * @throws JobNotFoundException if job is not found
     */
    public JobExecutionInfo execute(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        return jobExecutionPort.getJobExecutionById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job with ID '" + jobId + "' not found"));
    }
}

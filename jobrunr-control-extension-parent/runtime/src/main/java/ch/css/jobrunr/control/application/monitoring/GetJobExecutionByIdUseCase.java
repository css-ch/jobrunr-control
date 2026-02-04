package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Use Case: Returns a specific job execution by ID with job chain status evaluation.
 * <p>
 * This use case evaluates the entire job chain (parent + continuation jobs) to determine
 * the overall status. If there are continuation jobs (success/failure handlers), the status
 * is derived from the chain evaluation using JobRunr Pro job chaining rules.
 */
@ApplicationScoped
public class GetJobExecutionByIdUseCase {

    private final JobExecutionPort jobExecutionPort;

    @Inject
    public GetJobExecutionByIdUseCase(JobExecutionPort jobExecutionPort) {
        this.jobExecutionPort = jobExecutionPort;
    }

    /**
     * Returns a job execution by ID with job chain status evaluation.
     * <p>
     * The returned JobExecutionInfo contains the status of the entire job chain:
     * - If no continuation jobs exist, returns the parent job's status
     * - If continuation jobs exist, evaluates the chain and returns the overall status
     * - Chain is complete when all relevant leaf jobs have finished
     * - Chain succeeds when all executed leaf jobs succeeded
     * - Chain fails when any executed leaf job failed
     *
     * @param jobId Job ID
     * @return Job execution information with chain-evaluated status
     * @throws JobNotFoundException if job is not found
     */
    public JobExecutionInfo execute(UUID jobId) {
        if (jobId == null) {
            throw new IllegalArgumentException("jobId must not be null");
        }

        return jobExecutionPort.getJobChainExecutionById(jobId)
                .orElseThrow(() -> new JobNotFoundException("Job with ID '" + jobId + "' not found"));
    }
}

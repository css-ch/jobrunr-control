package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.JobExecutionInfo;
import ch.css.jobrunr.control.domain.JobExecutionPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use Case: Returns the job execution history.
 * Supports pagination and asynchronous loading of batch progress.
 */
@ApplicationScoped
public class GetJobExecutionHistoryUseCase {

    private final JobExecutionPort jobExecutionPort;

    @Inject
    public GetJobExecutionHistoryUseCase(JobExecutionPort jobExecutionPort) {
        this.jobExecutionPort = jobExecutionPort;
    }

    /**
     * Returns a paginated list of job executions.
     *
     * @return List of job execution information
     */
    public List<JobExecutionInfo> execute() {
        return jobExecutionPort.getJobExecutions();
    }
}


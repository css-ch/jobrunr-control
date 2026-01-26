package ch.css.jobrunr.control.application.monitoring;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Use Case: Returns all scheduled jobs.
 * Filters jobs for the scheduler overview and identifies externally triggerable jobs.
 */
@ApplicationScoped
public class GetScheduledJobsUseCase {

    private final JobSchedulerPort jobSchedulerPort;

    @Inject
    public GetScheduledJobsUseCase(JobSchedulerPort jobSchedulerPort) {
        this.jobSchedulerPort = jobSchedulerPort;
    }

    /**
     * Returns all scheduled jobs.
     *
     * @return List of all scheduled jobs
     */
    public List<ScheduledJobInfo> execute() {
        return jobSchedulerPort.getScheduledJobs();
    }

}


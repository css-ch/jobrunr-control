package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

/**
 * Use Case: Retrieves a single scheduled job.
 */
@ApplicationScoped
public class GetScheduledJobByIdUseCase {

    private final JobSchedulerPort jobSchedulerPort;

    @Inject
    public GetScheduledJobByIdUseCase(JobSchedulerPort jobSchedulerPort) {
        this.jobSchedulerPort = jobSchedulerPort;
    }

    /**
     * Executes the use case and returns a scheduled job.
     *
     * @param jobId The job ID
     * @return Optional with ScheduledJobInfo or empty if not found
     */
    public Optional<ScheduledJobInfo> execute(UUID jobId) {
        ScheduledJobInfo jobInfo = jobSchedulerPort.getScheduledJobById(jobId);
        return Optional.ofNullable(jobInfo);
    }
}

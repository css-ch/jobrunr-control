package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import java.time.Instant;
import java.util.*;

@ApplicationScoped
@Alternative
@Priority(1)
@IfBuildProfile("jobrunr-mock")
public class JobRunrScheduleAdapterMock implements JobSchedulerPort {
    private final Map<UUID, ScheduledJobInfo> jobs = new HashMap<>();

    @Override
    public UUID scheduleJob(JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        UUID jobId = UUID.randomUUID();
        ScheduledJobInfo jobInfo = new ScheduledJobInfo(jobId, jobName, jobDefinition.jobType(), scheduledAt, parameters, isExternalTrigger);
        jobs.put(jobId, jobInfo);
        return jobId;
    }

    @Override
    public void updateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        if (jobs.containsKey(jobId)) {
            ScheduledJobInfo jobInfo = new ScheduledJobInfo(jobId, jobName, jobDefinition.jobType(), scheduledAt, parameters, isExternalTrigger);
            jobs.put(jobId, jobInfo);
        }
    }

    @Override
    public void deleteScheduledJob(UUID jobId) {
        jobs.remove(jobId);
    }

    @Override
    public List<ScheduledJobInfo> getScheduledJobs() {
        return new ArrayList<>(jobs.values());
    }

    @Override
    public ScheduledJobInfo getScheduledJobById(UUID jobId) {
        return jobs.get(jobId);
    }

    @Override
    public void executeJobNow(UUID jobId) {
        executeJobNow(jobId, null);
    }

    @Override
    public void executeJobNow(UUID jobId, java.util.Map<String, Object> parameterOverrides) {
        deleteScheduledJob(jobId);
    }
}

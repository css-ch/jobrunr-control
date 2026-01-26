package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.infrastructure.jobrunr.JobParameterExtractor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.states.ScheduledState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JobRunr-based implementation of JobSchedulerPort.
 * Uses JobRunr's JobScheduler for job management.
 */
@ApplicationScoped
public class JobRunrSchedulerAdapter implements JobSchedulerPort {

    private static final Logger log = Logger.getLogger(JobRunrSchedulerAdapter.class);
    public static final Instant EXTERNAL_TRIGGER = Instant.parse("2999-12-31T23:59:59Z");

    private final JobScheduler jobScheduler;
    private final StorageProvider storageProvider;
    private final JobInvoker jobInvoker;

    @Inject
    public JobRunrSchedulerAdapter(
            JobScheduler jobScheduler,
            StorageProvider storageProvider,
            JobInvoker jobInvoker) {
        this.jobScheduler = jobScheduler;
        this.storageProvider = storageProvider;
        this.jobInvoker = jobInvoker;
    }

    @Override
    public UUID scheduleJob(JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        return createOrUpdateJob(null, jobDefinition, jobName, parameters, isExternalTrigger, scheduledAt);
    }

    @Override
    public void updateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        createOrUpdateJob(jobId, jobDefinition, jobName, parameters, isExternalTrigger, scheduledAt);
    }

    private UUID createOrUpdateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        try {
            if (isExternalTrigger) {
                // Set scheduled date to 31.12.2999 for externally triggerable jobs
                scheduledAt = EXTERNAL_TRIGGER;
            }
            // Create JobId and set job name
            JobId newJobId = jobInvoker.scheduleJob(
                    jobId,
                    jobName,
                    jobDefinition,
                    parameters,
                    scheduledAt
            );

            // Set job name via StorageProvider
            try {
                var job = storageProvider.getJobById(newJobId);
                job.setJobName(jobName);
                storageProvider.save(job);
            } catch (Exception e) {
                log.warnf("Could not set job name for job %s: %s", jobId, e.getMessage());
            }

            log.infof("Job scheduled: %s (ID: %s) for %s",
                    jobDefinition.jobType(), jobId, scheduledAt);

            return newJobId.asUUID();
        } catch (Exception e) {
            log.errorf("Error scheduling job: %s", jobDefinition.jobType(), e);
            throw new JobSchedulingException("Error scheduling job: " + jobDefinition.jobType(), e);
        }
    }


    @Override
    public void deleteScheduledJob(UUID jobId) {
        try {
            jobScheduler.delete(jobId);
            log.infof("Job deleted: %s", jobId);
        } catch (Exception e) {
            log.errorf(e, "Error deleting job: %s", jobId);
            throw new JobSchedulingException("Error deleting job: " + jobId, e);
        }
    }

    @Override
    public List<ScheduledJobInfo> getScheduledJobs() {
        try {
            // Create JobSearchRequest for SCHEDULED jobs
            var searchRequest = new org.jobrunr.storage.JobSearchRequest(StateName.SCHEDULED);

            // Create AmountRequest for jobs
            var amountRequest = new org.jobrunr.storage.navigation.AmountRequest(
                    "scheduledAt:ASC",
                    10000  // Maximum number
            );

            // Get all jobs in SCHEDULED state from StorageProvider
            List<org.jobrunr.jobs.Job> scheduledJobs = storageProvider.getJobList(
                    searchRequest,
                    amountRequest
            );

            log.debugf("Found scheduled jobs: %s", scheduledJobs.size());

            // Map to ScheduledJobInfo
            return scheduledJobs.stream()
                    .map(this::mapToScheduledJobInfo)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.errorf("Error retrieving scheduled jobs", e);
            return new ArrayList<>();
        }
    }

    @Override
    public ScheduledJobInfo getScheduledJobById(UUID jobId) {
        try {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            if (job != null && job.getState() == StateName.SCHEDULED) {
                return mapToScheduledJobInfo(job);
            }
            return null;
        } catch (Exception e) {
            log.errorf("Error retrieving job: %s", jobId, e);
            return null;
        }
    }

    private ScheduledJobInfo mapToScheduledJobInfo(org.jobrunr.jobs.Job job) {
        UUID jobId = job.getId();

        // Job name: Retrieved from JobRunr job name (set as a parameter during creation)
        String jobName = job.getJobName();
        if (jobName == null || jobName.isEmpty()) {
            jobName = "Unnamed Job";
        }

        // Job type: Extract simple class name of the job class
        String fullyQualifiedClassName = job.getJobDetails().getClassName();
        String jobType = extractSimpleClassName(fullyQualifiedClassName);

        // Get scheduledAt from ScheduledState
        Instant scheduledAt = job.getJobStates().stream()
                .filter(state -> state instanceof ScheduledState)
                .map(state -> (ScheduledState) state)
                .findFirst()
                .map(ScheduledState::getScheduledAt)
                .orElse(job.getCreatedAt());

        // Extract parameters
        Map<String, Object> parameters = JobParameterExtractor.extractParameters(job);

        // Check if externally triggerable
        boolean isExternallyTriggerable = isExternallyTriggerable(scheduledAt);

        return new ScheduledJobInfo(
                jobId,
                jobName,
                jobType,
                scheduledAt,
                parameters,
                isExternallyTriggerable
        );
    }

    private String extractSimpleClassName(String fullyQualifiedClassName) {
        int lastDotIndex = fullyQualifiedClassName.lastIndexOf('.');
        return lastDotIndex >= 0 ? fullyQualifiedClassName.substring(lastDotIndex + 1) : fullyQualifiedClassName;
    }

    @Override
    public void executeJobNow(UUID jobId) {
        executeJobNow(jobId, null);
    }

    @Override
    public void executeJobNow(UUID jobId, Map<String, Object> metadata) {
        try {
            // Get the existing job
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            if (job == null) {
                log.warnf("Job not found: %s", jobId);
                throw new JobSchedulingException("Job not found: " + jobId, null);
            }

            // Override parameters if provided
            if (metadata != null && !metadata.isEmpty()) {
                // Update job metadata with parameter overrides
                metadata.forEach((key, value) ->
                        job.getMetadata().put(key, value)
                );
                log.infof("Job %s will be executed with %s parameter override(s)", jobId, metadata.size());
            }

            // Set job to ENQUEUED state for immediate execution
            job.enqueue();

            // Save the updated job
            storageProvider.save(job);

            log.infof("Job is being executed immediately: %s", jobId);


        } catch (JobSchedulingException e) {
            log.errorf(e, "Error executing job immediately: %s", jobId);
            throw e;
        } catch (Exception e) {
            log.errorf(e, "Error executing job immediately: %s", jobId);
            throw new JobSchedulingException("Error executing job immediately: " + jobId, e);
        }
    }


    private boolean isExternallyTriggerable(Instant scheduledAt) {
        // Year 2999 indicates external triggers
        return scheduledAt != null &&
                scheduledAt.equals(EXTERNAL_TRIGGER);
    }

    /**
     * Exception for job scheduling errors.
     */
    public static class JobSchedulingException extends RuntimeException {
        public JobSchedulingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

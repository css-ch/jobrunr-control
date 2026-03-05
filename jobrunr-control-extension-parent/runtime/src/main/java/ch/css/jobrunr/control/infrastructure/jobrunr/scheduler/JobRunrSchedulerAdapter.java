package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobSchedulerPort;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import ch.css.jobrunr.control.domain.exceptions.DuplicateTemplateNameException;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.domain.exceptions.JobSchedulingException;
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

/**
 * JobRunr-based implementation of JobSchedulerPort.
 * Uses JobRunr's JobScheduler for job management.
 */
@ApplicationScoped
public class JobRunrSchedulerAdapter implements JobSchedulerPort {

    private static final Logger LOG = Logger.getLogger(JobRunrSchedulerAdapter.class);
    public static final Instant EXTERNAL_TRIGGER = Instant.parse("2999-12-31T23:59:59Z");

    private final JobScheduler jobScheduler;
    private final StorageProvider storageProvider;
    private final JobInvoker jobInvoker;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public JobRunrSchedulerAdapter(
            JobScheduler jobScheduler,
            StorageProvider storageProvider,
            JobInvoker jobInvoker,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobScheduler = jobScheduler;
        this.storageProvider = storageProvider;
        this.jobInvoker = jobInvoker;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    @Override
    public UUID scheduleJob(JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        return scheduleJob(jobDefinition, jobName, parameters, isExternalTrigger, scheduledAt, null);
    }

    @Override
    public UUID scheduleJob(JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt, List<String> additionalLabels) {
        return createOrUpdateJob(null, jobDefinition, jobName, parameters, isExternalTrigger, scheduledAt, additionalLabels);
    }

    @Override
    public void updateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt) {
        updateJob(jobId, jobDefinition, jobName, parameters, isExternalTrigger, scheduledAt, null);
    }

    @Override
    public void updateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt, List<String> additionalLabels) {
        createOrUpdateJob(jobId, jobDefinition, jobName, parameters, isExternalTrigger, scheduledAt, additionalLabels);
    }

    private UUID createOrUpdateJob(UUID jobId, JobDefinition jobDefinition, String jobName, Map<String, Object> parameters, boolean isExternalTrigger, Instant scheduledAt, List<String> additionalLabels) {
        if (additionalLabels != null && additionalLabels.contains("template")) {
            assertUniqueTemplateName(jobName, jobId);
        }
        return executeOrThrow("Error scheduling job: " + jobDefinition.jobType(), () -> {
            Instant effectiveScheduledAt = isExternalTrigger ? EXTERNAL_TRIGGER : scheduledAt;
            JobId newJobId = jobInvoker.scheduleJob(jobId, jobName, jobDefinition, parameters, effectiveScheduledAt, additionalLabels);

            try {
                var job = storageProvider.getJobById(newJobId);
                job.setJobName(jobName);
                storageProvider.save(job);
            } catch (Exception e) {
                LOG.warnf("Could not set job name for job %s: %s", jobId, e.getMessage());
            }

            LOG.infof("Job scheduled: %s (ID: %s) for %s",
                    jobDefinition.jobType(), newJobId, isExternalTrigger ? "external" : scheduledAt);
            return newJobId.asUUID();
        });
    }


    @Override
    public void deleteScheduledJob(UUID jobId) {
        executeOrThrow("Error deleting job: " + jobId, () -> {
            jobScheduler.delete(jobId);
            LOG.infof("Job deleted: %s", jobId);
            return null;
        });
    }

    @Override
    public List<ScheduledJobInfo> getScheduledJobs() {
        return executeOrDefault(new ArrayList<>(), "Error retrieving scheduled jobs", () -> {
            var searchRequest = new org.jobrunr.storage.JobSearchRequest(StateName.SCHEDULED);
            var amountRequest = new org.jobrunr.storage.navigation.AmountRequest("scheduledAt:ASC", 10000);
            List<org.jobrunr.jobs.Job> scheduledJobs = storageProvider.getJobList(searchRequest, amountRequest);
            LOG.debugf("Found scheduled jobs: %s", scheduledJobs.size());
            return scheduledJobs.stream().map(this::mapToScheduledJobInfo).toList();
        });
    }

    @Override
    public ScheduledJobInfo getScheduledJobById(UUID jobId) {
        return executeOrDefault(null, "Error retrieving job: " + jobId, () -> {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            if (job != null && job.getState() == StateName.SCHEDULED) {
                return mapToScheduledJobInfo(job);
            }
            return null;
        });
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

        // Get JobDefinition for this job type
        JobDefinition jobDefinition = jobDefinitionDiscoveryService.findJobByType(jobType)
                .orElseThrow(() -> new IllegalStateException("Job definition not found for type: " + jobType));

        // Get scheduledAt from ScheduledState
        Instant scheduledAt = job.getJobStates().stream()
                .filter(ScheduledState.class::isInstance)
                .map(ScheduledState.class::cast)
                .findFirst()
                .map(ScheduledState::getScheduledAt)
                .orElse(job.getCreatedAt());

        // Extract parameters
        Map<String, Object> parameters = JobParameterExtractor.extractParameters(job);

        // Check if externally triggerable
        boolean isExternallyTriggerable = isExternallyTriggerable(scheduledAt);

        // Extract labels
        List<String> labels = new ArrayList<>(job.getLabels());

        return new ScheduledJobInfo(
                jobId,
                jobName,
                jobDefinition,
                scheduledAt,
                parameters,
                isExternallyTriggerable,
                labels
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
        executeOrThrow("Error executing job immediately: " + jobId, () -> {
            org.jobrunr.jobs.Job job = storageProvider.getJobById(jobId);
            if (job == null) {
                throw new JobNotFoundException("Job with ID '" + jobId + "' not found");
            }
            if (metadata != null && !metadata.isEmpty()) {
                metadata.forEach((key, value) -> job.getMetadata().put(key, value));
                LOG.debugf("Job %s will be executed with %s parameter override(s)", jobId, metadata.size());
            }
            job.enqueue();
            storageProvider.save(job);
            LOG.infof("Job is being executed immediately: %s", jobId);
            return null;
        });
    }

    @Override
    public void updateJobParameters(UUID jobId, Map<String, Object> parameters) {
        executeOrThrow("Error updating job parameters: " + jobId, () -> {
            org.jobrunr.jobs.Job existingJob = storageProvider.getJobById(jobId);
            if (existingJob == null) {
                throw new JobNotFoundException("Job with ID '" + jobId + "' not found");
            }

            var jobDetails = existingJob.getJobDetails();
            String jobName = existingJob.getJobName();
            Instant scheduledAt = existingJob.getState() == StateName.SCHEDULED
                    ? existingJob.getJobStates().stream()
                            .filter(ScheduledState.class::isInstance)
                            .map(ScheduledState.class::cast)
                            .findFirst()
                            .map(ScheduledState::getScheduledAt)
                            .orElse(existingJob.getCreatedAt())
                    : existingJob.getCreatedAt();

            String simpleClassName = extractSimpleClassName(jobDetails.getClassName());
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.findJobByType(simpleClassName)
                    .orElseThrow(() -> new IllegalStateException("Job definition not found for: " + simpleClassName));

            jobInvoker.scheduleJob(jobId, jobName, jobDefinition, parameters, scheduledAt, new ArrayList<>(existingJob.getLabels()));
            LOG.infof("Updated parameters for job: %s", jobId);
            return null;
        });
    }

    /**
     * Throws DuplicateTemplateNameException if any other scheduled template already uses the given name.
     *
     * @param jobName   the candidate name
     * @param currentId the current job ID to exclude (null for new jobs)
     */
    private void assertUniqueTemplateName(String jobName, UUID currentId) {
        boolean duplicate = getScheduledJobs().stream()
                .filter(ScheduledJobInfo::isTemplate)
                .anyMatch(j -> jobName.equals(j.getJobName())
                        && (currentId == null || !currentId.equals(j.getJobId())));
        if (duplicate) {
            throw new DuplicateTemplateNameException(jobName);
        }
    }

    private boolean isExternallyTriggerable(Instant scheduledAt) {
        // Year 2999 indicates external triggers
        return scheduledAt != null &&
                scheduledAt.equals(EXTERNAL_TRIGGER);
    }

    private <T> T executeOrThrow(String errorMessage, java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (JobSchedulingException | JobNotFoundException | DuplicateTemplateNameException e) {
            throw e;
        } catch (Exception e) {
            LOG.errorf(e, errorMessage);
            throw new JobSchedulingException(errorMessage, e);
        }
    }

    private <T> T executeOrDefault(T defaultValue, String errorMessage, java.util.function.Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            LOG.errorf(e, errorMessage);
            return defaultValue;
        }
    }
}

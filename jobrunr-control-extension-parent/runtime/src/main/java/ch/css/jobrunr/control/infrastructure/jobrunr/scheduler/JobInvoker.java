package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobRequestOnFailureFactory;
import ch.css.jobrunr.control.annotations.JobRequestOnSuccessFactory;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSettings;
import ch.css.jobrunr.control.domain.exceptions.JobSchedulingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobBuilder;
import org.jobrunr.scheduling.JobRequestId;
import org.jobrunr.scheduling.JobRequestScheduler;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.scheduling.JobBuilder.aBatchJob;
import static org.jobrunr.scheduling.JobBuilder.aJob;

/**
 * Helper class for creating and scheduling JobRequests.
 * Converts parameter maps to JobRequest objects using Jackson ObjectMapper.
 */
@ApplicationScoped
public class JobInvoker {

    private static final Logger LOG = Logger.getLogger(JobInvoker.class);

    private final JobRequestScheduler jobScheduler;
    private final ObjectMapper objectMapper;

    @Inject
    public JobInvoker(JobRequestScheduler jobScheduler, ObjectMapper objectMapper) {
        this.jobScheduler = jobScheduler;
        this.objectMapper = objectMapper;
    }

    /**
     * Schedules a job with dynamic parameters using JobRequestScheduler.
     * Uses Jackson ObjectMapper to create JobRequest instances from parameter maps.
     *
     * @param jobId            Optional JobId (null for new JobId)
     * @param jobName          Name of the job
     * @param jobDefinition    Job definition containing metadata
     * @param parameters       Job parameters
     * @param scheduledAt      Time of execution
     * @param additionalLabels Additional labels to add to the job
     * @return JobId
     */
    public JobId scheduleJob(UUID jobId, String jobName, JobDefinition jobDefinition, Map<String, Object> parameters, Instant scheduledAt, List<String> additionalLabels) {
        try {
            // Load the JobRequest class
            String className = jobDefinition.jobRequestTypeName();
            // Load the JobRequest class
            Class<? extends JobRequest> jobRequestClass = Thread.currentThread().getContextClassLoader().loadClass(className).asSubclass(JobRequest.class);
            // Convert parameters to JobRequest using Jackson
            JobRequest jobRequest = objectMapper.convertValue(parameters, jobRequestClass);

            // Schedule the job with JobRequestScheduler
            JobBuilder jobBuilder = jobDefinition.isBatchJob() ? aBatchJob() : aJob();
            jobBuilder
                    .withId(jobId)
                    .withName(jobName)
                    .scheduleAt(scheduledAt)
                    .withJobRequest(jobRequest);

            // Combine default labels with additional labels
            List<String> allLabels = new ArrayList<>();
            allLabels.add("jobtype:" + jobDefinition.jobType());
            if (additionalLabels != null && !additionalLabels.isEmpty()) {
                allLabels.addAll(additionalLabels);
            }

            applyJobSettings(jobBuilder, jobDefinition.jobSettings(), allLabels);
            JobRequestId jobRequestId = jobScheduler.createOrReplace(jobBuilder);
            if (jobRequest instanceof JobRequestOnSuccessFactory jobRequestOnSuccessFactory) {
                jobRequestId.continueWith(jobRequestOnSuccessFactory.createOnSuccessJobRequest(jobRequestId, jobRequest));
            }
            if (jobRequest instanceof JobRequestOnFailureFactory jobRequestOnFailureFactory) {
                jobRequestId.onFailure(jobRequestOnFailureFactory.createOnFailureJobRequest(jobRequestId, jobRequest));
            }
            LOG.infof("Job scheduled successfully: %s (batch=%s) with JobId: %s", jobDefinition.jobSettings().name(), jobDefinition.jobType(), jobRequestId);
            return jobRequestId;
        } catch (ClassNotFoundException e) {
            LOG.errorf("Failed to load JobRequest class: %s", jobDefinition.jobRequestTypeName(), e);
            throw new JobSchedulingException("JobRequest class not found: " + jobDefinition.jobRequestTypeName(), e);
        } catch (Exception e) {
            LOG.errorf("Failed to schedule job: %s (batch=%s)", jobDefinition.jobSettings().name(), jobDefinition.jobType(), e);
            throw new JobSchedulingException("Failed to schedule job: " + jobDefinition.jobSettings().name(), e);
        }
    }

    /**
     * Schedules a job without additional labels (backward compatibility).
     */
    public JobId scheduleJob(UUID jobId, String jobName, JobDefinition jobDefinition, Map<String, Object> parameters, Instant scheduledAt) {
        return scheduleJob(jobId, jobName, jobDefinition, parameters, scheduledAt, null);
    }

    /**
     * Applies all job settings from JobSettings to the JobBuilder.
     */
    private void applyJobSettings(JobBuilder jobBuilder, JobSettings settings, List<String> additionalLabels) {
        applyName(jobBuilder, settings);
        applyRetries(jobBuilder, settings);
        applyLabels(jobBuilder, settings, additionalLabels);
        applyQueue(jobBuilder, settings);
        applyServerTag(settings);
        applyMutex(jobBuilder, settings);
        applyRateLimiter(jobBuilder, settings);
        applyProcessTimeout(jobBuilder, settings);
        applyDeleteOnSuccess(jobBuilder, settings);
        applyDeleteOnFailure(jobBuilder, settings);
    }

    private void applyName(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.name())) {
            jobBuilder.withName(settings.name());
        }
    }

    private void applyRetries(JobBuilder jobBuilder, JobSettings settings) {
        if (settings.retries() != ConfigurableJob.NBR_OF_RETRIES_NOT_PROVIDED) {
            jobBuilder.withAmountOfRetries(settings.retries());
        }
    }

    private void applyLabels(JobBuilder jobBuilder, JobSettings settings, List<String> additionalLabels) {
        List<String> settingsLabels = settings.labels();
        boolean hasSettingsLabels = settingsLabels != null && !settingsLabels.isEmpty();
        boolean hasAdditionalLabels = additionalLabels != null && !additionalLabels.isEmpty();

        if (hasSettingsLabels || hasAdditionalLabels) {
            List<String> allLabels = new ArrayList<>();
            if (hasSettingsLabels) {
                allLabels.addAll(settingsLabels);
            }
            if (hasAdditionalLabels) {
                allLabels.addAll(additionalLabels);
            }
            jobBuilder.withLabels(allLabels);
        }
    }

    private void applyQueue(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.queue())) {
            jobBuilder.withQueue(settings.queue());
        }
    }

    private void applyServerTag(JobSettings settings) {
        if (isNotEmpty(settings.runOnServerWithTag())) {
            LOG.warnf("Server tag support not yet implemented: %s", settings.runOnServerWithTag());
        }
    }

    private void applyMutex(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.mutex())) {
            jobBuilder.withMutex(settings.mutex());
        }
    }

    private void applyRateLimiter(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.rateLimiter())) {
            jobBuilder.withRateLimiter(settings.rateLimiter());
        }
    }

    private void applyProcessTimeout(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.processTimeOut())) {
            try {
                java.time.Duration timeout = java.time.Duration.parse(settings.processTimeOut());
                jobBuilder.withProcessTimeOut(timeout);
            } catch (Exception e) {
                LOG.warnf(e, "Invalid process timeout format: %s", settings.processTimeOut());
            }
        }
    }

    private void applyDeleteOnSuccess(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.deleteOnSuccess())) {
            jobBuilder.withDeleteOnSuccess(settings.deleteOnSuccess());
        }
    }

    private void applyDeleteOnFailure(JobBuilder jobBuilder, JobSettings settings) {
        if (isNotEmpty(settings.deleteOnFailure())) {
            jobBuilder.withDeleteOnFailure(settings.deleteOnFailure());
        }
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}

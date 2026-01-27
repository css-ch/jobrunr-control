package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobRequestOnFailureFeactory;
import ch.css.jobrunr.control.annotations.JobRequestOnSuccessFactory;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobSettings;
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

    private static final Logger log = Logger.getLogger(JobInvoker.class);

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
            if (jobRequest instanceof JobRequestOnFailureFeactory jobRequestOnFailureFeactory) {
                jobRequestId.onFailure(jobRequestOnFailureFeactory.createOnFailureJobRequest(jobRequestId, jobRequest));
            }
            log.infof("Job scheduled successfully: %s (batch=%s) with JobId: %s", jobDefinition.jobSettings().name(), jobDefinition.jobType(), jobRequestId);
            return jobRequestId;
        } catch (ClassNotFoundException e) {
            log.errorf("Failed to load JobRequest class: %s", jobDefinition.jobRequestTypeName(), e);
            throw new RuntimeException("JobRequest class not found: " + jobDefinition.jobRequestTypeName(), e);
        } catch (Exception e) {
            log.errorf("Failed to schedule job: %s (batch=%s)", jobDefinition.jobSettings().name(), jobDefinition.jobType(), e);
            throw new RuntimeException("Failed to schedule job: " + jobDefinition.jobSettings().name(), e);
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
        // Apply name if provided
        if (settings.name() != null && !settings.name().isEmpty()) {
            jobBuilder.withName(settings.name());
        }

        // Apply retries if specified
        if (settings.retries() != ConfigurableJob.NBR_OF_RETRIES_NOT_PROVIDED) {
            jobBuilder.withAmountOfRetries(settings.retries());
        }

        // Apply labels if provided (merge settings labels with additional labels)
        if ((settings.labels() != null && !settings.labels().isEmpty()) || (additionalLabels != null && !additionalLabels.isEmpty())) {
            java.util.List<String> allLabels = new java.util.ArrayList<>();
            if (settings.labels() != null) {
                allLabels.addAll(settings.labels());
            }
            if (additionalLabels != null) {
                allLabels.addAll(additionalLabels);
            }
            jobBuilder.withLabels(allLabels);
        }

        // Apply job filters if provided
        if (settings.jobFilters() != null && !settings.jobFilters().isEmpty()) {
            // Note: JobBuilder.withJobFilter may not be available in all versions
            // Load and apply job filter classes
            for (String filterClassName : settings.jobFilters()) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends org.jobrunr.jobs.filters.JobFilter> filterClass =
                            (Class<? extends org.jobrunr.jobs.filters.JobFilter>)
                                    Thread.currentThread().getContextClassLoader().loadClass(filterClassName);
                    // jobBuilder.withJobFilter(filterClass); // Method may not exist in JobRunr 8.4.1
                    log.warnf("JobFilter support not yet implemented: %s", filterClassName);
                } catch (ClassNotFoundException e) {
                    log.warnf("Could not load JobFilter class: %s", filterClassName, e);
                }
            }
        }

        // Apply queue if provided
        if (settings.queue() != null && !settings.queue().isEmpty()) {
            jobBuilder.withQueue(settings.queue());
        }

        // Apply server tag if provided
        if (settings.runOnServerWithTag() != null && !settings.runOnServerWithTag().isEmpty()) {
            // jobBuilder.runOnServerWithTag(settings.runOnServerWithTag()); // Method may not exist in JobRunr 8.4.1
            log.warnf("Server tag support not yet implemented: %s", settings.runOnServerWithTag());
        }

        // Apply mutex if provided
        if (settings.mutex() != null && !settings.mutex().isEmpty()) {
            jobBuilder.withMutex(settings.mutex());
        }

        // Apply rate limiter if provided
        if (settings.rateLimiter() != null && !settings.rateLimiter().isEmpty()) {
            jobBuilder.withRateLimiter(settings.rateLimiter());
        }

        // Apply process timeout if provided
        if (settings.processTimeOut() != null && !settings.processTimeOut().isEmpty()) {
            try {
                java.time.Duration timeout = java.time.Duration.parse(settings.processTimeOut());
                jobBuilder.withProcessTimeOut(timeout);
            } catch (Exception e) {
                log.warnf(e, "Invalid process timeout format: %s", settings.processTimeOut());
            }
        }

        // Apply delete on success if provided
        if (settings.deleteOnSuccess() != null && !settings.deleteOnSuccess().isEmpty()) {
            jobBuilder.withDeleteOnSuccess(settings.deleteOnSuccess());
        }

        // Apply delete on failure if provided
        if (settings.deleteOnFailure() != null && !settings.deleteOnFailure().isEmpty()) {
            jobBuilder.withDeleteOnFailure(settings.deleteOnFailure());
        }
    }
}

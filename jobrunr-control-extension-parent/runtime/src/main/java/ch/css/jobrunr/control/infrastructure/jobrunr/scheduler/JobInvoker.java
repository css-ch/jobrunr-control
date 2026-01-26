package ch.css.jobrunr.control.infrastructure.jobrunr.scheduler;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.jobrunr.scheduling.JobBuilder.aBatchJob;

/**
 * Helper class for creating and scheduling JobRequests.
 * Converts parameter maps to JobRequest objects using Jackson ObjectMapper.
 */
@ApplicationScoped
public class JobInvoker {

    private static final Logger log = LoggerFactory.getLogger(JobInvoker.class);

    private final JobRequestScheduler jobRequestScheduler;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;
    private final ObjectMapper objectMapper;

    @Inject
    public JobInvoker(JobRequestScheduler jobRequestScheduler, JobDefinitionDiscoveryService jobDefinitionDiscoveryService, ObjectMapper objectMapper) {
        this.jobRequestScheduler = jobRequestScheduler;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
        this.objectMapper = objectMapper;
    }

    /**
     * Schedules a job with dynamic parameters using JobRequestScheduler.
     * Uses Jackson ObjectMapper to create JobRequest instances from parameter maps.
     *
     * @param jobId       Optional JobId (null for new JobId)
     * @param jobType     Type of the job to schedule
     * @param parameters  Job parameters
     * @param scheduledAt Time of execution
     * @return JobId
     */
    public JobId scheduleJob(UUID jobId, String jobType, Map<String, Object> parameters, Boolean isBatchJob, Instant scheduledAt) {
        try {
            // Load the JobRequest class
            JobDefinition jobDefinition = jobDefinitionDiscoveryService.findJobByType(jobType)
                    .orElseThrow(() -> new IllegalArgumentException("Job type '" + jobType + "' not found"));
            String className = jobDefinition.jobRequestTypeName();

            // Load the JobRequest class
            Class<? extends JobRequest> jobRequestClass = Thread.currentThread().getContextClassLoader().loadClass(className).asSubclass(JobRequest.class);

            // Convert parameters to JobRequest using Jackson
            JobRequest jobRequest = objectMapper.convertValue(parameters, jobRequestClass);

            // Schedule the job with JobRequestScheduler
            JobId resultId;
            if (isBatchJob) {
                resultId = jobRequestScheduler.createOrReplace(
                        aBatchJob()
                                .withId(jobId)
                                .scheduleAt(scheduledAt)
                                .withJobRequest(jobRequest));
            } else {
                resultId = jobRequestScheduler.scheduleOrReplace(jobId, scheduledAt, jobRequest);
            }

            log.info("Job scheduled successfully: {} (batch={}) with JobId: {}", jobType, isBatchJob, resultId);
            return resultId;
        } catch (Exception e) {
            throw new RuntimeException("Error scheduling job: " + jobType, e);
        }
    }
}

package ch.css.jobrunr.control.infrastructure.scheduler;

import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionDiscoveryAdapter;
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

/**
 * Helper class for creating and scheduling JobRequests.
 * Converts parameter maps to JobRequest objects and uses JobRequestScheduler.
 */
@ApplicationScoped
public class JobInvoker {

    private static final Logger log = LoggerFactory.getLogger(JobInvoker.class);

    private final JobRequestScheduler jobRequestScheduler;
    private final JobDefinitionDiscoveryAdapter jobRunrJobDiscoveryAdapter;

    @Inject
    public JobInvoker(JobRequestScheduler jobRequestScheduler, JobDefinitionDiscoveryAdapter jobRunrJobDiscoveryAdapter) {
        this.jobRequestScheduler = jobRequestScheduler;
        this.jobRunrJobDiscoveryAdapter = jobRunrJobDiscoveryAdapter;
    }

    /**
     * Schedules a job with dynamic parameters using JobRequestScheduler.
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
            Class<? extends JobRequest> requestClass = jobRunrJobDiscoveryAdapter.getJobRequestClass(jobType);

            // Create a new instance of the request class
            JobRequest jobRequest = createRequestInstance(requestClass, parameters);

            // Check if this is a batch job
            boolean isImmediate = scheduledAt == null || scheduledAt.isBefore(Instant.now().plusSeconds(5));

            // Schedule the job with JobRequestScheduler
            JobId resultId;
            if (jobId != null) {
                // Update existing job - always use scheduleOrReplace
                resultId = jobRequestScheduler.scheduleOrReplace(jobId, scheduledAt, jobRequest);
            } else if (isBatchJob && isImmediate) {
                // For immediate batch jobs, use startBatch() to create the batch parent
                resultId = jobRequestScheduler.startBatch(jobRequest);
            } else {
                // For non-batch jobs or scheduled batch jobs, use schedule()
                // Note: Scheduled batch jobs will create their batch parent when they execute
                resultId = jobRequestScheduler.schedule(scheduledAt != null ? scheduledAt : Instant.now(), jobRequest);
            }

            log.info("Job scheduled successfully: {} (batch={}, immediate={}) with JobId: {}",
                    jobType, isBatchJob, isImmediate, resultId);
            return resultId;

        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Job request class not found: " + jobType, e);
        } catch (Exception e) {
            throw new RuntimeException("Error scheduling job: " + jobType, e);
        }
    }

    private <T extends JobRequest> T createRequestInstance(Class<? extends T> requestClass, Map<String, Object> parameters)
            throws Exception {
        // Get all fields of the record (record components)
        java.lang.reflect.RecordComponent[] recordComponents = requestClass.getRecordComponents();

        if (recordComponents == null || recordComponents.length == 0) {
            // Fallback for non-record classes: use no-arg constructor
            return requestClass.getDeclaredConstructor().newInstance();
        }

        // Collect the constructor parameters in the correct order
        Object[] constructorArgs = new Object[recordComponents.length];
        Class<?>[] parameterTypes = new Class<?>[recordComponents.length];

        for (int i = 0; i < recordComponents.length; i++) {
            java.lang.reflect.RecordComponent component = recordComponents[i];
            String fieldName = component.getName();
            Class<?> fieldType = component.getType();

            parameterTypes[i] = fieldType;

            // Get the value from the parameter map
            Object value = parameters.get(fieldName);

            // Convert the value to the correct type
            constructorArgs[i] = convertToType(value, fieldType);

            if (value != null && constructorArgs[i] == null) {
                log.warn("Could not convert parameter '{}' of type {} to target type {}",
                        fieldName, value.getClass().getSimpleName(), fieldType.getSimpleName());
            }
        }

        // Call the canonical constructor with the parameters
        java.lang.reflect.Constructor<? extends T> constructor = requestClass.getDeclaredConstructor(parameterTypes);
        return constructor.newInstance(constructorArgs);
    }

    private Object convertToType(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isInstance(value)) {
            return value;
        }

        // String to various types
        if (value instanceof String str) {
            if (targetType == Integer.class || targetType == int.class) {
                return Integer.parseInt(str);
            } else if (targetType == Long.class || targetType == long.class) {
                return Long.parseLong(str);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return Boolean.parseBoolean(str);
            } else if (targetType == java.time.LocalDate.class) {
                return java.time.LocalDate.parse(str);
            } else if (targetType == java.time.LocalDateTime.class) {
                return java.time.LocalDateTime.parse(str);
            } else if (targetType == java.time.Instant.class) {
                return java.time.Instant.parse(str);
            }
        }

        // Number to various types
        if (value instanceof Number num) {
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            }
        }

        return value;
    }
}

package ch.css.jobrunr.control.infrastructure.discovery;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * JobRunr-based implementation of JobDefinitionDiscoveryService.
 * Scans all JobRequest classes with @OpsJob annotation and extracts job definitions.
 */
@ApplicationScoped
public class JobDefinitionDiscoveryAdapter implements JobDefinitionDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(JobDefinitionDiscoveryAdapter.class);

    @Any
    Instance<ConfigurableJob<?>> jobHandlers;

    private volatile Set<JobDefinition> jobDefinitionsCache;
    private final Map<String, Class<? extends JobRequest>> jobRequestClassMap = new HashMap<>();

    @Inject
    public JobDefinitionDiscoveryAdapter() {
        log.info("JobRunrJobDiscoveryAdapter initialized");
    }

    /**
     * Called on application startup to trigger job discovery.
     */
    void onStart(@Observes StartupEvent ev) {
        log.info("Application Startup - triggering job discovery...");
        getAllJobDefinitions();
    }

    /**
     * Returns all discovered job definitions.
     *
     * @return list of job definitions
     */
    @Override
    public List<JobDefinition> getAllJobDefinitions() {
        if (jobDefinitionsCache == null) {
            synchronized (this) {
                if (jobDefinitionsCache == null) {
                    jobDefinitionsCache = discoverJobs();
                }
            }
        }
        return new ArrayList<>(jobDefinitionsCache);
    }

    /**
     * Finds a job definition by its type.
     *
     * @param type the job type
     * @return optional job definition
     */
    @Override
    public Optional<JobDefinition> findJobByType(String type) {
        return getAllJobDefinitions().stream()
                .filter(jd -> Objects.equals(jd.type(), type))
                .findFirst();
    }

    /**
     * Scans all JobRequest classes for @OpsJob annotations and creates job definitions.
     */
    private Set<JobDefinition> discoverJobs() {
        Set<JobDefinition> definitions = new HashSet<>();
        log.info("Starting job discovery...");

        // Discover jobs via JobRequestHandler beans
        for (ConfigurableJob<?> handler : jobHandlers) {
            try {
                Class<? extends JobRequest> requestClass = handler.getJobRequestType();
                if (requestClass == null || !JobRequest.class.isAssignableFrom(requestClass)) {
                    log.warn("JobRequestHandler {} returned null for getJobRequestType()", handler.getClass().getName());
                    continue;
                }
                log.info("Found Configurable Job: {} with {}", handler.getClass().getSimpleName(), requestClass.getName());
                definitions.add(createJobDefinition(handler, requestClass));
                jobRequestClassMap.put(getJobType(handler), requestClass);

            } catch (Exception e) {
                log.warn("Could not extract request class from handler: {}", handler.getClass().getName(), e);
            }
        }

        log.info("Scanned: {} jobs discovered", definitions.size());
        return definitions;
    }


    /**
     * Processes a JobRequest class and creates a JobDefinition.
     */
    private JobDefinition createJobDefinition(ConfigurableJob<?> handler, Class<?> requestClass) {
        return new JobDefinition(
                getJobType(handler),
                isBatchJob(handler),
                extractParametersFromRequest(requestClass)
        );
    }

    private Boolean isBatchJob(ConfigurableJob<?> handler) {
        return handler.isBatchJob();
    }

    /**
     * Extracts parameter definitions from a JobRequest class (from its fields).
     */
    private Set<JobParameter> extractParametersFromRequest(Class<?> requestClass) {
        Set<JobParameter> parameters = new HashSet<>();

        // Iterate over all fields of the request class
        for (Field field : requestClass.getDeclaredFields()) {
            // Skip static fields and serialVersionUID
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            String paramName = field.getName();
            Class<?> paramType = field.getType();

            JobParameterType type = mapJavaTypeToJobParameterType(paramType);
            boolean required = !isOptionalType(paramType);
            Object defaultValue = null;

            JobParameter jobParameter = new JobParameter(paramName, type, required, defaultValue);
            parameters.add(jobParameter);
        }

        return parameters;
    }

    /**
     * Maps Java types to JobParameterType.
     */
    private JobParameterType mapJavaTypeToJobParameterType(Class<?> javaType) {
        if (javaType == String.class) {
            return JobParameterType.STRING;
        } else if (javaType == Integer.class || javaType == int.class ||
                javaType == Long.class || javaType == long.class) {
            return JobParameterType.INTEGER;
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return JobParameterType.BOOLEAN;
        } else if (javaType == LocalDate.class) {
            return JobParameterType.DATE;
        } else if (javaType == LocalDateTime.class || javaType == Instant.class) {
            return JobParameterType.DATETIME;
        } else {
            // Fallback to String
            log.warn("Unknown parameter type: {}, using STRING as fallback", javaType);
            return JobParameterType.STRING;
        }
    }

    /**
     * Returns the job type for the given handler.
     *
     * @param handler the configurable job handler
     * @return job type string
     */
    public String getJobType(ConfigurableJob<?> handler) {
        return handler.getJobType();
    }

    /**
     * Checks if a type is optional (e.g., Object wrappers instead of primitives).
     */
    private boolean isOptionalType(Class<?> type) {
        return !type.isPrimitive();
    }

    /**
     * Returns the JobRequest class for the given job type.
     *
     * @param jobType the job type string
     * @return JobRequest class
     */
    public Class<? extends JobRequest> getJobRequestClass(String jobType) {
        return jobRequestClassMap.get(jobType);
    }

}

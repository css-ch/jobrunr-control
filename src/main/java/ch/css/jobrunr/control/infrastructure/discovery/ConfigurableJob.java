package ch.css.jobrunr.control.infrastructure.discovery;

import ch.css.jobrunr.control.infrastructure.discovery.annotation.BatchJob;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.lang.reflect.Method;
import java.util.Arrays;

public interface ConfigurableJob<T extends JobRequest> extends JobRequestHandler<T> {

    default boolean isBatchJob() {
        return findRunMethod().isAnnotationPresent(BatchJob.class);
    }

    default String getJobType() {
        Method runMethod = findRunMethod();
        Job jobAnnotation = runMethod.getAnnotation(Job.class);

        if (jobAnnotation != null && !jobAnnotation.name().isBlank()) {
            return jobAnnotation.name();
        }
        // Fallback: Class name (use superclass/user class for proxies)
        return getUserClass().getSimpleName();
    }

    @SuppressWarnings("unchecked")
    default Class<T> getJobRequestType() {
        // The type of T is simply the first parameter of the run method
        return (Class<T>) findRunMethod().getParameterTypes()[0];
    }

    /**
     * Finds the actual 'run' method in the user class.
     * Bypasses issues with generics erasure and proxies.
     */
    private Method findRunMethod() {
        Class<?> currentClass = getUserClass();
        return Arrays.stream(currentClass.getMethods())
                .filter(m -> "run".equals(m.getName()))
                .filter(m -> !m.isBridge()) // Important: Ignore generated bridge methods (run(Object))
                .filter(m -> m.getParameterCount() == 1)
                .filter(m -> JobRequest.class.isAssignableFrom(m.getParameterTypes()[0]))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find valid run(JobRequest) method in " + currentClass.getName()));
    }

    /**
     * Helper method to find the real class behind Quarkus/CDI proxies.
     * Quarkus often uses subclassing (MyBean_Subclass extends MyBean).
     */
    private Class<?> getUserClass() {
        Class<?> clazz = this.getClass();
        // Simple check: If the class name contains "_Subclass" or similar,
        // it's likely a Quarkus proxy. Checking the superclass is often safer.
        if (clazz.getName().contains("_Subclass") || clazz.getName().contains("$$")) {
            return clazz.getSuperclass();
        }
        return clazz;
    }
}
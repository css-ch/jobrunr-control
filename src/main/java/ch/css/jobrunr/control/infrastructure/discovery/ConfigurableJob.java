package ch.css.jobrunr.control.infrastructure.discovery;

import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Extended JobRequestHandler interface that provides access to the JobRequest type at runtime.
 *
 * @param <T> The JobRequest type this handler processes
 */
public interface ConfigurableJob<T extends JobRequest> extends JobRequestHandler<T> {

    default String getJobType() {
        try {
            Class<?> implementingClass = this.getClass();
            Class<T> jobRequestType = this.getJobRequestType();

            // Find the run method that takes the JobRequest type parameter
            Method runMethod = implementingClass.getMethod("run", jobRequestType);
            Job jobAnnotation = runMethod.getAnnotation(Job.class);

            if (jobAnnotation != null && jobAnnotation.name() != null && !jobAnnotation.name().isEmpty()) {
                return jobAnnotation.name();
            }
            return implementingClass.getSimpleName();
        } catch (NoSuchMethodException e) {
            LoggerFactory.getLogger(ConfigurableJob.class).error("Could not find run method in class: {}", this.getClass().getName(), e);
        }
        return null;
    }

    /**
     * Gets the actual JobRequest class type for this handler.
     * Uses reflection to extract the generic type parameter.
     *
     * @return The Class object representing the JobRequest type
     * @throws IllegalStateException if the type cannot be determined
     */
    default Class<T> getJobRequestType() {
        // Get the class of the implementing instance
        Class<?> currentClass = this.getClass();

        // Search through the class hierarchy and interfaces for the generic type
        Class<T> requestType = extractGenericType(currentClass);

        if (requestType != null) {
            return requestType;
        }

        throw new IllegalStateException(
                "Could not determine JobRequest type for " + currentClass.getName() +
                        ". Make sure the class directly implements Jorrr<YourRequestType>."
        );
    }

    /**
     * Recursively extracts the generic type parameter from the class hierarchy.
     */
    @SuppressWarnings("unchecked")
    private Class<T> extractGenericType(Class<?> clazz) {
        // Check all implemented interfaces
        Type[] genericInterfaces = clazz.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                // Check if this is Jorrr or JobRequestHandler interface
                Type rawType = parameterizedType.getRawType();
                if (rawType.equals(ConfigurableJob.class) || rawType.equals(JobRequestHandler.class)) {
                    Type[] typeArguments = parameterizedType.getActualTypeArguments();
                    if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                        return (Class<T>) typeArguments[0];
                    }
                }
            }
        }

        // Check the generic superclass
        Type genericSuperclass = clazz.getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericSuperclass;
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            if (typeArguments.length > 0 && typeArguments[0] instanceof Class) {
                return (Class<T>) typeArguments[0];
            }
        }

        // Recursively check parent class
        Class<?> superclass = clazz.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return extractGenericType(superclass);
        }

        return null;
    }
}

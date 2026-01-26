package ch.css.jobrunr.control.infrastructure.discovery;

import ch.css.jobrunr.control.domain.JobDefinition;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Runtime recorder for storing job metadata computed at build time.
 * Creates field accessors at STATIC_INIT time using pre-computed metadata from build-time scanning.
 * This minimizes runtime reflection for better native image compatibility.
 */
@Recorder
public class JobDefinitionRecorder {

    public void registerJobMetadata(Set<JobDefinition> jobDefinitions) {
        JobDefinitionRegistry.INSTANCE.setDefinitions(jobDefinitions);
    }

    public void registerOpenApiAvailability(boolean hasOpenApi) {
        JobDefinitionRegistry.INSTANCE.setOpenApiAvailable(hasOpenApi);
    }

    /**
     * Singleton registry to hold job definitions and build-time configuration at runtime.
     */
    public static class JobDefinitionRegistry {
        public static final JobDefinitionRegistry INSTANCE = new JobDefinitionRegistry();

        private final Map<String, JobDefinition> definitionsByHandlerClass = new HashMap<>();
        private boolean openApiAvailable = false;

        private JobDefinitionRegistry() {
        }

        void setDefinitions(Set<JobDefinition> jobDefinitions) {
            definitionsByHandlerClass.clear();
            for (JobDefinition jobDefinition : jobDefinitions) {
                definitionsByHandlerClass.put(jobDefinition.handlerClassName(), jobDefinition);
            }
        }

        void setOpenApiAvailable(boolean available) {
            this.openApiAvailable = available;
        }

        public boolean isOpenApiAvailable() {
            return openApiAvailable;
        }

        public JobDefinition getDefinition(String handlerClassName) {
            return definitionsByHandlerClass.get(handlerClassName);
        }

        public Collection<JobDefinition> getAllDefinitions() {
            return definitionsByHandlerClass.values();
        }
    }
}

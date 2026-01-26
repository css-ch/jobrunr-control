package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionRecorder;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * Build-time processor that scans for ConfigurableJob implementations
 * and extracts metadata to avoid runtime reflection.
 */
public class JobDiscoveryProcessor {

    private static final DotName JOB_REQUEST = DotName.createSimple(JobRequest.class.getName());

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void discoverAndRegisterJobs(
            CombinedIndexBuildItem indexBuildItem,
            JobDefinitionRecorder recorder) {
        recorder.registerJobMetadata(JobDefinitionIndexScanner.findJobSpecifications(indexBuildItem.getIndex()));
    }

    /**
     * Registers all JobRequest subclasses for reflection.
     * This allows Jackson ObjectMapper to serialize/deserialize them at runtime.
     */
    @BuildStep
    void registerJobRequestsForReflection(
            CombinedIndexBuildItem indexBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // Find all classes that implement JobRequest
        var jobRequestClasses = indexBuildItem.getIndex()
                .getAllKnownImplementations(JOB_REQUEST)
                .stream()
                .map(ClassInfo::name)
                .map(DotName::toString)
                .toArray(String[]::new);

        if (jobRequestClasses.length > 0) {
            // Register all JobRequest implementations for reflection
            // Enable methods, fields, and constructors for Jackson serialization/deserialization
            reflectiveClass.produce(
                    ReflectiveClassBuildItem.builder(jobRequestClasses)
                            .methods()
                            .fields()
                            .constructors()
                            .build()
            );
        }
    }
}

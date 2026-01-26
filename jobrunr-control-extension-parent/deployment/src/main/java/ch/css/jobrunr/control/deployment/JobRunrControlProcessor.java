package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.adapter.ui.DashboardTemplateExtensions;
import ch.css.jobrunr.control.adapter.ui.DashboardUrlUtils;
import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionRecorder;
import ch.css.jobrunr.control.infrastructure.quarkus.BuildTimeConfigurationAdapter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class JobRunrControlProcessor {

    private static final String FEATURE = "jobrunr-control";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Required because used in Qute templates.
     */
    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        DashboardUrlUtils.class,
                        DashboardTemplateExtensions.class,
                        BuildTimeConfigurationAdapter.class
                )
                .setUnremovable()
                .build();
    }

    /**
     * Detect if SmallRye OpenAPI extension is present and configure dashboard.
     * Uses Quarkus Capabilities API to check for the quarkus-smallrye-openapi extension.
     * The information is passed to runtime via recorder.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void detectOpenApi(Capabilities capabilities, JobDefinitionRecorder recorder) {
        boolean hasOpenApi = capabilities.isPresent(Capability.SMALLRYE_OPENAPI);
        recorder.registerOpenApiAvailability(hasOpenApi);
    }
}



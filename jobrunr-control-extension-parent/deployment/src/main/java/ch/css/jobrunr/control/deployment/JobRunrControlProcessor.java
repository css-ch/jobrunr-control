package ch.css.jobrunr.control.deployment;

import java.util.List;

import ch.css.jobrunr.control.adapter.rest.JobControlResource;
import ch.css.jobrunr.control.adapter.ui.*;
import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionRecorder;
import ch.css.jobrunr.control.infrastructure.jobrunr.filters.ParameterCleanupJobFilter;
import ch.css.jobrunr.control.infrastructure.quarkus.BuildTimeConfigurationAdapter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class JobRunrControlProcessor {

    private static final String FEATURE = "jobrunr-control";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register JAX-RS controllers as beans.
     * Setting unremovable ensures @RolesAllowed annotations are processed.
     */
    @BuildStep
    AdditionalBeanBuildItem registerControllers() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        DashboardController.class,
                        ScheduledJobsController.class,
                        TemplatesController.class,
                        JobExecutionsController.class,
                        JobControlResource.class
                )
                .setUnremovable()
                .build();
    }

    /**
     * Register default HTTP authentication policy for all JobRunr Control paths.
     * Sets "authenticated" as the default policy so consuming apps don't need to add
     * these properties manually. The consuming app can override with:
     * quarkus.http.auth.permission.jobrunr-control.policy=permit
     */
    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> registerDefaultHttpAuthPolicy() {
        return List.of(
                new RunTimeConfigurationDefaultBuildItem(
                        "quarkus.http.auth.permission.jobrunr-control.paths",
                        "/q/jobrunr-control,/q/jobrunr-control/*"
                ),
                new RunTimeConfigurationDefaultBuildItem(
                        "quarkus.http.auth.permission.jobrunr-control.policy",
                        "authenticated"
                )
        );
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
                        BuildTimeConfigurationAdapter.class,
                        ParameterCleanupJobFilter.class
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



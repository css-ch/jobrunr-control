package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.adapter.rest.JobControlResource;
import ch.css.jobrunr.control.adapter.ui.*;
import ch.css.jobrunr.control.infrastructure.discovery.JobDefinitionRecorder;
import ch.css.jobrunr.control.infrastructure.jobrunr.filters.ParameterCleanupJobFilter;
import ch.css.jobrunr.control.infrastructure.quarkus.BuildTimeConfigurationAdapter;
import ch.css.jobrunr.control.security.JobRunrControlRoleAugmentor;
import ch.css.jobrunr.control.security.JobRunrDashboardUserContextFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.util.List;

public class JobRunrControlProcessor {

    private static final String FEATURE = "jobrunr-control";
    private static final String UI_BASE = "jobrunr-control";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Register UI controllers and REST resources as beans.
     * Setting unremovable ensures that controllers referenced only by the Vert.x routes
     * (and no longer by JAX-RS scanning) survive ArC removal.
     */
    @BuildStep
    AdditionalBeanBuildItem registerControllers() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(
                        JobDetailsController.class,
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
     * Register Vert.x routes for the JobRunr Control UI under the non-application root path
     * (default {@code /q}). This decouples the UI from JAX-RS {@code @ApplicationPath} so the
     * dashboard always lives at {@code /q/jobrunr-control} regardless of consumer configuration.
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void registerUiRoutes(
            NonApplicationRootPathBuildItem nonApp,
            JobRunrControlRouteRecorder recorder,
            io.quarkus.deployment.annotations.BuildProducer<RouteBuildItem> routes) {

        // ---- Dashboard root ----
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE)
                .handler(recorder.dashboardIndex())
                .handlerType(HandlerType.BLOCKING)
                .displayOnNotFoundPage("JobRunr Control Dashboard")
                .build());

        // ---- Scheduled Jobs ----
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/scheduled", HttpMethodFilter.GET)
                .handler(recorder.scheduledJobsIndex())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/scheduled", HttpMethodFilter.POST)
                .handler(recorder.scheduledJobsCreate())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/scheduled/table")
                .handler(recorder.scheduledJobsTable())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/scheduled/modal/new")
                .handler(recorder.scheduledJobsNewModal())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/scheduled/modal/:id/edit")
                .handler(recorder.scheduledJobsEditModal())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/scheduled/modal/parameters")
                .handler(recorder.scheduledJobsParametersModal())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/scheduled/:id", HttpMethodFilter.PUT)
                .handler(recorder.scheduledJobsUpdate())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/scheduled/:id", HttpMethodFilter.DELETE)
                .handler(recorder.scheduledJobsDelete())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/scheduled/:id/execute", HttpMethodFilter.POST)
                .handler(recorder.scheduledJobsExecute())
                .handlerType(HandlerType.BLOCKING)
                .build());

        // ---- Templates ----
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/templates", HttpMethodFilter.GET)
                .handler(recorder.templatesIndex())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/templates", HttpMethodFilter.POST)
                .handler(recorder.templatesCreate())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/templates/table")
                .handler(recorder.templatesTable())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/templates/modal/new")
                .handler(recorder.templatesNewModal())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/templates/modal/:id/edit")
                .handler(recorder.templatesEditModal())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/templates/modal/parameters")
                .handler(recorder.templatesParametersModal())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/templates/:id", HttpMethodFilter.PUT)
                .handler(recorder.templatesUpdate())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/templates/:id", HttpMethodFilter.DELETE)
                .handler(recorder.templatesDelete())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/templates/:id/clone", HttpMethodFilter.POST)
                .handler(recorder.templatesClone())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .routeFunction(UI_BASE + "/templates/:id/start", HttpMethodFilter.POST)
                .handler(recorder.templatesStart())
                .handlerType(HandlerType.BLOCKING)
                .build());

        // ---- Execution History ----
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history")
                .handler(recorder.historyIndex())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/table")
                .handler(recorder.historyTable())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/:id/batch-progress")
                .handler(recorder.historyBatchProgress())
                .handlerType(HandlerType.BLOCKING)
                .build());

        // ---- Dashboard ----
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/details")
                .handler(recorder.jobDetailsIndex())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/details/recap")
                .handler(recorder.jobDetailsRecap())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/details/parameter")
                .handler(recorder.jobDetailsParameter())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/details/parameter/download")
                .handler(recorder.jobDetailsParameterDownload())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/details/messages")
                .handler(recorder.jobDetailsMessages())
                .handlerType(HandlerType.BLOCKING)
                .build());
        routes.produce(nonApp.routeBuilder()
                .route(UI_BASE + "/history/details/messages/download")
                .handler(recorder.jobDetailsMessagesDownload())
                .handlerType(HandlerType.BLOCKING)
                .build());
    }

    /**
     * Register a default HTTP authentication policy for the JobRunr Control UI paths.
     * Paths are resolved from the non-application root path so they stay in sync with the
     * actually mounted routes (including any custom {@code quarkus.http.root-path} or
     * {@code quarkus.http.non-application-root-path}).
     * <p>
     * The REST API ({@code JobControlResource}) is still a JAX-RS resource and is therefore
     * prefixed by {@code quarkus.http.root-path} and {@code quarkus.rest.path}/{@code @ApplicationPath}.
     * Consumers must configure the API auth permission separately.
     */
    @BuildStep
    List<RunTimeConfigurationDefaultBuildItem> registerDefaultHttpAuthPolicy(
            NonApplicationRootPathBuildItem nonApp) {
        String uiPath = nonApp.resolvePath(UI_BASE);
        String uiWildcard = uiPath + "/*";
        return List.of(
                new RunTimeConfigurationDefaultBuildItem(
                        "quarkus.http.auth.permission.jobrunr-control.paths",
                        uiPath + "," + uiWildcard
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
                        DashboardPaths.class,
                        BuildTimeConfigurationAdapter.class,
                        ParameterCleanupJobFilter.class,
                        JobRunrControlRoleAugmentor.class
                )
                .setUnremovable()
                .build();
    }

    /**
     * Register {@link JobRunrDashboardUserContextFilter} as an explicit CDI bean so it
     * takes precedence over the {@code @DefaultBean} produced by the JobRunr Pro Quarkus
     * extension ({@code JobRunrQuarkusAnonymousAuthenticationFilter}). Without this, the
     * dashboard shows "You do not have access to the JobRunr Pro Dashboard."
     */
    @BuildStep
    AdditionalBeanBuildItem registerDashboardAuthFilter() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(JobRunrDashboardUserContextFilter.class)
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
    void detectOpenApi(
            Capabilities capabilities,
            LaunchModeBuildItem launchMode,
            JobDefinitionRecorder recorder) {
        Config config = ConfigProvider.getConfig();
        boolean hasOpenApi = capabilities.isPresent(Capability.SMALLRYE_OPENAPI);
        boolean alwaysInclude = config
                .getOptionalValue("quarkus.swagger-ui.always-include", Boolean.class)
                .orElse(false);
        boolean swaggerUiActive = hasOpenApi && (launchMode.getLaunchMode() != LaunchMode.NORMAL || alwaysInclude);
        String openApiUrl = null;
        if (swaggerUiActive) {
            String swaggerUiPath = config.getOptionalValue("quarkus.swagger-ui.path", String.class)
                    .orElse("swagger-ui");
            String nonApplicationRootPath = config.getOptionalValue("quarkus.http.non-application-root-path", String.class)
                    .orElse("/q");
            openApiUrl = buildSwaggerUiUrl(nonApplicationRootPath, swaggerUiPath);
        }
        recorder.registerOpenApiAvailability(swaggerUiActive, openApiUrl);
    }

    private static String buildSwaggerUiUrl(
            String nonApplicationRootPath,
            String swaggerUiPath) {
        if(swaggerUiPath.startsWith("/")) {
            return swaggerUiPath;
        }
        String root = removeLeadingAndTrailingSlash(nonApplicationRootPath);
        String path = removeLeadingAndTrailingSlash(swaggerUiPath);
        if (path.isEmpty()) {
            return "/" + root;
        }
        return "/" + root + "/" + path;
    }

    private static String removeLeadingAndTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String result = value.startsWith("/") ? value.substring(1) : value;
        return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }
}

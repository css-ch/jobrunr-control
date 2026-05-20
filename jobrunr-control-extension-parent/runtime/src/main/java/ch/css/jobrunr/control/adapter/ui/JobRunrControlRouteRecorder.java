package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Recorder producing Vert.x {@link Handler}s for the JobRunr Control UI routes.
 * <p>
 * Routes are registered via {@code NonApplicationRootPathBuildItem} in the deployment
 * module, so the UI lives at {@code /q/jobrunr-control/*} regardless of the consuming
 * application's {@code @ApplicationPath}.
 * <p>
 * Each handler activates a CDI request context via
 * {@link UiRoutingSupport#withRequestContext(RoutingContext, Runnable)} so request-scoped
 * beans (e.g. {@code SecurityIdentity}) are available to the controller and to Qute
 * template extensions during rendering.
 */
@Recorder
public class JobRunrControlRouteRecorder {

    // ---------------- Dashboard ----------------

    public Handler<RoutingContext> dashboardIndex() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(DashboardController.class).handleIndex(ctx));
    }

    // ---------------- Scheduled Jobs ----------------

    public Handler<RoutingContext> scheduledJobsIndex() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleIndex(ctx));
    }

    public Handler<RoutingContext> scheduledJobsTable() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleTable(ctx));
    }

    public Handler<RoutingContext> scheduledJobsNewModal() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleNewModal(ctx));
    }

    public Handler<RoutingContext> scheduledJobsEditModal() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleEditModal(ctx));
    }

    public Handler<RoutingContext> scheduledJobsParametersModal() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleParametersModal(ctx));
    }

    public Handler<RoutingContext> scheduledJobsCreate() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleCreate(ctx));
    }

    public Handler<RoutingContext> scheduledJobsUpdate() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleUpdate(ctx));
    }

    public Handler<RoutingContext> scheduledJobsDelete() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleDelete(ctx));
    }

    public Handler<RoutingContext> scheduledJobsExecute() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(ScheduledJobsController.class).handleExecute(ctx));
    }

    // ---------------- Templates ----------------

    public Handler<RoutingContext> templatesIndex() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleIndex(ctx));
    }

    public Handler<RoutingContext> templatesTable() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleTable(ctx));
    }

    public Handler<RoutingContext> templatesNewModal() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleNewModal(ctx));
    }

    public Handler<RoutingContext> templatesEditModal() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleEditModal(ctx));
    }

    public Handler<RoutingContext> templatesParametersModal() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleParametersModal(ctx));
    }

    public Handler<RoutingContext> templatesCreate() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleCreate(ctx));
    }

    public Handler<RoutingContext> templatesUpdate() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleUpdate(ctx));
    }

    public Handler<RoutingContext> templatesDelete() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleDelete(ctx));
    }

    public Handler<RoutingContext> templatesClone() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleClone(ctx));
    }

    public Handler<RoutingContext> templatesStart() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(TemplatesController.class).handleStart(ctx));
    }

    // ---------------- Execution History ----------------

    public Handler<RoutingContext> historyIndex() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(JobExecutionsController.class).handleIndex(ctx));
    }

    public Handler<RoutingContext> historyTable() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(JobExecutionsController.class).handleTable(ctx));
    }

    public Handler<RoutingContext> historyBatchProgress() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(JobExecutionsController.class).handleBatchProgress(ctx));
    }

    // ---------------- Dashboard ----------------

    public Handler<RoutingContext> dashboardBatchIndex() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(DashboardController.class).handleBatchIndex(ctx));
    }

    public Handler<RoutingContext> dashboardRecap() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(DashboardController.class).handleDashboardRecap(ctx));
    }

    public Handler<RoutingContext> dashboardParameter() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(DashboardController.class).handleDashboardParameter(ctx));
    }

    public Handler<RoutingContext> dashboardMessages() {
        return ctx -> UiRoutingSupport.withRequestContext(ctx,
                () -> controller(DashboardController.class).handleDashboardMessages(ctx));
    }

    private static <T> T controller(Class<T> type) {
        return Arc.container().instance(type).get();
    }
}

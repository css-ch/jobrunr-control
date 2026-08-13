package ch.css.jobrunr.control.infrastructure.jobrunr.filters;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jobrunr.configuration.JobRetentionConfiguration;
import org.jobrunr.jobs.filters.DeleteFilter;
import org.jobrunr.quarkus.autoconfigure.JobRunrRuntimeConfiguration;

import java.time.Duration;

/**
 * Keeps JobRunr's built-in delete filter available when application filters are
 * present. Quarkus disables JobRunr's {@code @DefaultBean} producer as soon as
 * another {@code JobFilter} bean (such as {@link ParameterCleanupJobFilter}) is
 * discovered.
 */
@ApplicationScoped
public class DeleteFilterProducer {

    private final JobRunrRuntimeConfiguration configuration;

    @Inject
    public DeleteFilterProducer(JobRunrRuntimeConfiguration configuration) {
        this.configuration = configuration;
    }

    @Produces
    @Singleton
    DeleteFilter deleteFilter() {
        var jobs = configuration.jobs();
        var backgroundJobServer = configuration.backgroundJobServer();

        Duration deleteSucceededJobsAfter = jobs.deleteSucceededJobsAfter()
                .or(() -> backgroundJobServer.deleteSucceededJobsAfter())
                .orElse(JobRetentionConfiguration.DEFAULT_DELETE_SUCCEEDED_JOBS_DURATION);
        Duration deleteFailedJobsAfter = jobs.deleteFailedJobsAfter()
                .or(() -> backgroundJobServer.deleteFailedJobsAfter())
                .orElse(null);
        Duration permanentlyDeleteDeletedJobsAfter = jobs.permanentlyDeleteDeletedJobsAfter()
                .or(() -> backgroundJobServer.permanentlyDeleteDeletedJobsAfter())
                .orElse(JobRetentionConfiguration.DEFAULT_PERMANENTLY_DELETE_JOBS_DURATION);

        return new DeleteFilter(
                deleteSucceededJobsAfter,
                deleteFailedJobsAfter,
                permanentlyDeleteDeletedJobsAfter);
    }
}

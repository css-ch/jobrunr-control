package ch.css.jobrunr.control.jobs.purejobrunr;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.scheduling.JobRequestScheduler;
import org.jobrunr.scheduling.cron.Cron;

/**
 * Registers a recurring pure JobRunr job on application startup so that JobRunr
 * Control's scheduled-jobs view is exercised against a handler that lacks the
 * {@code @ConfigurableJob} annotation.
 */
@ApplicationScoped
public class PureJobRunrJobScheduler {

    private static final Logger LOG = Logger.getLogger(PureJobRunrJobScheduler.class);
    private static final String RECURRING_JOB_ID = "pure-jobrunr-recurring";

    private final JobRequestScheduler jobRequestScheduler;

    @Inject
    public PureJobRunrJobScheduler(JobRequestScheduler jobRequestScheduler) {
        this.jobRequestScheduler = jobRequestScheduler;
    }

    void onStartup(@Observes StartupEvent event) {
        jobRequestScheduler.scheduleRecurrently(
                RECURRING_JOB_ID,
                Cron.hourly(),
                new PureJobRunrJobRequest("scheduled-at-startup"));
        LOG.infof("Registered pure JobRunr recurring job '%s' (cron: hourly)", RECURRING_JOB_ID);
    }
}

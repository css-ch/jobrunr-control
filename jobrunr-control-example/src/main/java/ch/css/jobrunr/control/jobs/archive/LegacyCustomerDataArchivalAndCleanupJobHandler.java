package ch.css.jobrunr.control.jobs.archive;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Demonstrates the {@code jobType} override on {@link ConfigurableJob}. The handler's simple
 * class name is 46 characters long — more than the 37-character budget reserved for the
 * {@code jobtype:} label on scheduled jobs. Without the override the build would fail; with
 * the override the class keeps its descriptive name while the label stays within JobRunr's
 * 45-character cap.
 */
@ApplicationScoped
public class LegacyCustomerDataArchivalAndCleanupJobHandler
        implements JobRequestHandler<LegacyCustomerDataArchivalAndCleanupJobRequest> {

    private static final Logger LOG = Logger.getLogger(LegacyCustomerDataArchivalAndCleanupJobHandler.class);

    @Override
    @ConfigurableJob(
            name = "Legacy Customer Data Archival And Cleanup",
            jobType = "LegacyArchival"
    )
    public void run(LegacyCustomerDataArchivalAndCleanupJobRequest request) {
        LOG.infof("Archiving legacy customer data before %s (dryRun=%s)",
                request.archiveBefore(), request.dryRun());
    }
}

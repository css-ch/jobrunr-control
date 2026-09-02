package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.domain.JobResultPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Job with various parameter types.
 * Demonstrates String, MULTILINE, Integer, Double, Boolean, Date, DateTime (at all
 * precision levels: minutes, seconds, milliseconds), Enum, and Multi-Enum parameters.
 */
@ApplicationScoped
public class ParameterDemoJob implements JobRequestHandler<ParameterDemoJobRequest> {

    private static final Logger LOG = Logger.getLogger(ParameterDemoJob.class);
    private final JobResultPort jobResultPort;

    @Inject
    public ParameterDemoJob(JobResultPort jobResultPort) {
        this.jobResultPort = jobResultPort;
    }

    /**
     * Executes the parameter demo job, logging all provided parameters.
     *
     * @param request the job request containing all parameter values
     */
    @Override
    @ConfigurableJob()
    public void run(ParameterDemoJobRequest request) {
        LOG.infof("String parameter: %s", request.stringParameter());
        LOG.infof("Multiline parameter: %s", request.multilineParameter());
        LOG.infof("Integer parameter: %s", request.integerParameter());
        LOG.infof("Double parameter: %s", request.doubleParameter());
        LOG.infof("Boolean parameter: %s", request.booleanParameter());
        LOG.infof("Date parameter: %s", request.dateParameter());
        LOG.infof("DateTime parameter: %s", request.dateTimeParameter());
        LOG.infof("DateTime (seconds precision) parameter: %s", request.dateTimeSecondsParameter());
        LOG.infof("DateTime (milliseconds precision) parameter: %s", request.dateTimeMillisParameter());
        LOG.infof("Enum parameter: %s", request.enumParameter());
        LOG.infof("Multi-Enum parameter: %s", request.multiEnumParameter());

        jobResultPort.storeResult(0, "Parameter demo job completed successfully");
    }
}


package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Job mit verschiedenen Parameter-Typen.
 * Demonstriert String, MULTILINE, Integer, Double, Boolean, Date, Enum und Multi-Enum Parameter.
 */
@ApplicationScoped
public class ParameterDemoJob implements JobRequestHandler<ParameterDemoJobRequest> {

    private static final Logger LOG = Logger.getLogger(ParameterDemoJob.class);

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
        LOG.infof("Enum parameter: %s", request.enumParameter());
        LOG.infof("Multi-Enum parameter: %s", request.multiEnumParameter());
    }
}


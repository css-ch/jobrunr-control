package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Job mit verschiedenen Parameter-Typen.
 * Demonstriert String, MULTILINE, Integer, Boolean, Date, Enum und Multi-Enum Parameter.
 */
@ApplicationScoped
public class ParameterDemoJob implements JobRequestHandler<ParameterDemoJobRequest> {

    @Override
    @ConfigurableJob()
    public void run(ParameterDemoJobRequest request) throws Exception {
        JobDashboardLogger log = jobContext().logger();
        log.info(String.format("String parameter: %s", request.stringParameter()));
        log.info(String.format("Multiline parameter: %s", request.multilineParameter()));
        log.info(String.format("Integer parameter: %s", request.integerParameter()));
        log.info(String.format("Boolean parameter: %s", request.booleanParameter()));
        log.info(String.format("Date parameter: %s", request.dateParameter()));
        log.info(String.format("DateTime parameter: %s", request.dateTimeParameter()));
        log.info(String.format("Enum parameter: %s", request.enumParameter()));
        log.info(String.format("Multi-Enum parameter: %s", request.multiEnumParameter()));
    }
}


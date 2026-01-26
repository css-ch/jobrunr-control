package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.context.JobDashboardLogger;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

/**
 * Job mit verschiedenen Parameter-Typen.
 * Demonstriert String, Integer, Boolean, Date und Enum Parameter.
 */
@ApplicationScoped
public class ParameterDemoJob implements JobRequestHandler<ParameterDemoJobRequest> {

    @Override
    @ConfigurableJob()
    @Job(name = "Parameter Demo Job", labels = "sdfsd") // TODO Remove
    public void run(ParameterDemoJobRequest request) throws Exception {
        JobDashboardLogger log = jobContext().logger();
        log.info(String.format("String parameter: %s", request.stringParameter()));
        log.info(String.format("Integer parameter: %s", request.integerParameter()));
        log.info(String.format("Boolean parameter: %s", request.booleanParameter()));
        log.info(String.format("Date parameter: %s", request.dateParameter()));
        log.info(String.format("DateTime parameter: %s", request.dateTimeParameter()));
        log.info(String.format("Enum parameter: %s", request.enumParameter()));
    }
}


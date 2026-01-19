package ch.css.jobrunr.control.jobs;

import ch.css.jobrunr.control.infrastructure.discovery.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.context.JobDashboardLogger;

/**
 * Job mit verschiedenen Parameter-Typen.
 * Demonstriert String, Integer, Boolean und Date Parameter.
 */
@ApplicationScoped
public class ParameterDemoJob implements ConfigurableJob<ParameterDemoJobRequest> {

    @Override
    public void run(ParameterDemoJobRequest request) throws Exception {
        JobDashboardLogger log = jobContext().logger();
        log.info(String.format("String parameter: %s", request.stringParameter()));
        log.info(String.format("Integer parameter: %s", request.integerParameter()));
        log.info(String.format("Boolean parameter: %s", request.booleanParameter()));
        log.info(String.format("Date parameter: %s", request.dateParameter()));
        log.info(String.format("DateTime parameter: %s", request.dateTimeParameter()));
    }
}


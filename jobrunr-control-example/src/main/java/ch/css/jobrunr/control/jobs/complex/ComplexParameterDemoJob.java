package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@ApplicationScoped
public class ComplexParameterDemoJob implements JobRequestHandler<ComplexParameterDemoJobRequest> {
    @Override
    @ConfigurableJob(isBatch = true)
    public void run(ComplexParameterDemoJobRequest complexParameterDemoJobRequest) throws Exception {

    }
}

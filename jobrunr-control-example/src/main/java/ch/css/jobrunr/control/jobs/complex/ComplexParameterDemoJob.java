package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

@ApplicationScoped
public class ComplexParameterDemoJob implements JobRequestHandler<ComplexParameterDemoJobRequest> {
    @Override
    @ConfigurableJob(name="ComplexParameterDemoJob-Custom-Name", isBatch = true, resultPageUrl = "http://{host}:{port}/mybatch/result/{jobId}?stage={stage}&startDate={startDate}&endDate={endDate}")
    public void run(ComplexParameterDemoJobRequest complexParameterDemoJobRequest) throws Exception {

    }
}

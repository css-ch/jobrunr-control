package ch.css.jobrunr.control.jobs.batch.postprocess;


import ch.css.jobrunr.control.jobs.batch.ExampleBatchJobProcessScenario;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.util.UUID;

public record ExampleBatchSuccessRequest(UUID workflowRootJobId,
                                         ExampleBatchJobProcessScenario processScenario) implements JobRequest {

    @Override
    public Class<ExampleBatchSuccess> getJobRequestHandler() {
        return ExampleBatchSuccess.class;
    }
}

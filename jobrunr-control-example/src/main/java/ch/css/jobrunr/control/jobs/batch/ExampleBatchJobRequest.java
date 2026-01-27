package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobRequestOnFailureFeactory;
import ch.css.jobrunr.control.annotations.JobRequestOnSuccessFactory;
import ch.css.jobrunr.control.jobs.batch.postprocess.ExampleBatchFailureRequest;
import ch.css.jobrunr.control.jobs.batch.postprocess.ExampleBatchSuccessRequest;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.scheduling.JobRequestId;

public record ExampleBatchJobRequest(
        @JobParameterDefinition(defaultValue = "100") Integer numberOfJunks,
        Integer junkSize,
        @JobParameterDefinition(defaultValue = "true") Boolean simulateErrors) implements JobRequest, JobRequestOnSuccessFactory, JobRequestOnFailureFeactory {
    @Override
    public Class<ExampleBatchJob> getJobRequestHandler() {
        return ExampleBatchJob.class;
    }

    /**
     * Example of creating a new JobRequest to be executed on success of the batch job
     */
    @Override
    public JobRequest createOnSuccessJobRequest(JobRequestId jobRequestId, JobRequest jobRequest) {
        // Simply return the original job request for batch processing
        return new ExampleBatchSuccessRequest(
                jobRequest
        );
    }

    /**
     * Example of creating a new JobRequest to be executed on success of the batch job
     */
    @Override
    public JobRequest createOnFailureJobRequest(JobRequestId jobRequestId, JobRequest jobRequest) {
        return new ExampleBatchFailureRequest(
                jobRequest
        );
    }
}

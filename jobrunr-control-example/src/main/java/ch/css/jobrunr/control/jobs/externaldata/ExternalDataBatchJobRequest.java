package ch.css.jobrunr.control.jobs.externaldata;

import ch.css.jobrunr.control.annotations.JobParameterSet;
import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * Example batch job request using external parameter storage via @JobParameterSet.
 * <p>
 * This demonstrates how to use external parameters for batch jobs with potentially
 * large parameter sets. The parameters are stored in the database and retrieved
 * in the job handler using the job's own UUID via ThreadLocalJobContext.
 * <p>
 * This job simulates a data processing workflow with configurable batch size and error simulation.
 */
@JobParameterSet(parameterSetClass = ExternalDataBatchParameters.class)
public record ExternalDataBatchJobRequest() implements JobRequest {

    @Override
    public Class<ExternalDataBatchJob> getJobRequestHandler() {
        return ExternalDataBatchJob.class;
    }
}

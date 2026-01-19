package ch.css.jobrunr.control.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;

public record SimpleReportJobRequest() implements JobRequest {
    @Override
    public Class<SimpleReportJob> getJobRequestHandler() {
        return SimpleReportJob.class;
    }
}

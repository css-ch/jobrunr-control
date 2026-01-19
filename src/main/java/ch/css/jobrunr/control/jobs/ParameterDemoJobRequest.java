package ch.css.jobrunr.control.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ParameterDemoJobRequest(String stringParameter, Integer integerParameter, Boolean booleanParameter,
                                      LocalDate dateParameter, LocalDateTime dateTimeParameter) implements JobRequest {
    @Override
    public Class<ParameterDemoJob> getJobRequestHandler() {
        return ParameterDemoJob.class;
    }
}

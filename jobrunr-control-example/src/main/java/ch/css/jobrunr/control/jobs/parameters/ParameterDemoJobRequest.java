package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ParameterDemoJobRequest(
        @JobParameterDefinition(defaultValue = "Default String")
        String stringParameter,
        @JobParameterDefinition(defaultValue = "42")
        Integer integerParameter,
        @JobParameterDefinition(defaultValue = "true")
        Boolean booleanParameter,
        @JobParameterDefinition(defaultValue = "2024-01-01")
        LocalDate dateParameter,
        @JobParameterDefinition(defaultValue = "2024-01-01T12:00:00")
        LocalDateTime dateTimeParameter,
        @JobParameterDefinition(defaultValue = "OPTION_B")
        EnumParameter enumParameter) implements JobRequest {
    @Override
    public Class<ParameterDemoJob> getJobRequestHandler() {
        return ParameterDemoJob.class;
    }
}

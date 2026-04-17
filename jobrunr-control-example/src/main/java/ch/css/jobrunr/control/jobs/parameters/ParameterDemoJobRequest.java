package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

public record ParameterDemoJobRequest(
        @JobParameterDefinition(required = false, defaultValue = "Default String")
        String stringParameter,
        @JobParameterDefinition(required = false, type = "MULTILINE", defaultValue = "Line 1\nLine 2\nLine 3")
        String multilineParameter,
        @JobParameterDefinition(required = false, defaultValue = "42")
        Integer integerParameter,
        @JobParameterDefinition(required = false, defaultValue = "3.14159")
        Double doubleParameter,
        @JobParameterDefinition(required = false, defaultValue = "true")
        Boolean booleanParameter,
        @JobParameterDefinition(required = false, defaultValue = "2024-01-01")
        LocalDate dateParameter,
        @JobParameterDefinition(required = false, defaultValue = "2024-01-01T12:00:00")
        LocalDateTime dateTimeParameter,
        @JobParameterDefinition(required = false, defaultValue = "OPTION_B")
        EnumParameter enumParameter,
        @JobParameterDefinition(required = false, defaultValue = "OPTION_A,OPTION_C")
        EnumSet<EnumParameter> multiEnumParameter) implements JobRequest {
    @Override
    public Class<ParameterDemoJob> getJobRequestHandler() {
        return ParameterDemoJob.class;
    }
}

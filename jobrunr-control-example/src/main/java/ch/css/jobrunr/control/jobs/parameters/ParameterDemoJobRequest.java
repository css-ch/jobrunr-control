package ch.css.jobrunr.control.jobs.parameters;

import ch.css.jobrunr.control.annotations.DateTimePrecision;
import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

public record ParameterDemoJobRequest(
        @JobParameterDefinition(required = false, defaultValue = "Default String", order = 0)
        String stringParameter,
        @JobParameterDefinition(required = false, type = "MULTILINE", defaultValue = "Line 1\nLine 2\nLine 3", order = 1)
        String multilineParameter,
        @JobParameterDefinition(required = false, defaultValue = "42", order = 2)
        Integer integerParameter,
        @JobParameterDefinition(required = false, defaultValue = "3.14159", order = 3)
        Double doubleParameter,
        @JobParameterDefinition(required = false, defaultValue = "true", order = 4)
        Boolean booleanParameter,
        @JobParameterDefinition(required = false, defaultValue = "2024-01-01", order = 5)
        LocalDate dateParameter,
        @JobParameterDefinition(required = false, defaultValue = "2024-01-01T12:00:00", order = 6)
        LocalDateTime dateTimeParameter,
        @JobParameterDefinition(required = false, defaultValue = "2024-01-01T12:00:00", dateTimePrecision = DateTimePrecision.SECONDS, order = 7)
        LocalDateTime dateTimeSecondsParameter,
        @JobParameterDefinition(required = false, defaultValue = "2024-01-01T12:00:00.000", dateTimePrecision = DateTimePrecision.MILLISECONDS, order = 8)
        LocalDateTime dateTimeMillisParameter,
        @JobParameterDefinition(required = false, defaultValue = "OPTION_B", order = 9)
        EnumParameter enumParameter,
        @JobParameterDefinition(required = false, defaultValue = "OPTION_A,OPTION_C", order = 10)
        EnumSet<EnumParameter> multiEnumParameter) implements JobRequest {
    @Override
    public Class<ParameterDemoJob> getJobRequestHandler() {
        return ParameterDemoJob.class;
    }
}

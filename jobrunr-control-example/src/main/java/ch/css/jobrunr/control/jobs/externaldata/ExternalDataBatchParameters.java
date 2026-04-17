package ch.css.jobrunr.control.jobs.externaldata;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.jobs.parameters.EnumParameter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

public record ExternalDataBatchParameters(
        @JobParameterDefinition(
                required = false,
                defaultValue = "100"
        )
        int numberOfChildJobs,
        @JobParameterDefinition(
                required = false,
                defaultValue = "Default External String"
        )
        String stringExternalParameter,
        @JobParameterDefinition(
                required = false,
                type = "MULTILINE",
                defaultValue = "Line 1\nLine 2\nLine 3"
        )
        String notesExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "100"
        )
        int integerExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "3.14159"
        )
        double doubleExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "true"
        )
        boolean booleanExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "2026-01-01"
        )
        LocalDate dateExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "2026-01-01T12:00:00"
        )
        LocalDateTime dateTimeExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "OPTION_B"
        )
        EnumParameter enumExternalParameter,
        @JobParameterDefinition(
                required = false,
                defaultValue = "OPTION_A,OPTION_C"
        )
        EnumSet<EnumParameter> multiEnumExternalParameter
) {
}

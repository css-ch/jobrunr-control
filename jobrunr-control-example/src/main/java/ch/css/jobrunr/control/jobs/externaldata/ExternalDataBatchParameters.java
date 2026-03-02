package ch.css.jobrunr.control.jobs.externaldata;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.jobs.parameters.EnumParameter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

public record ExternalDataBatchParameters(
        @JobParameterDefinition(
                defaultValue = "100"
        )
        int numberOfChildJobs,
        @JobParameterDefinition(
                defaultValue = "Default External String"
        )
        String stringExternalParameter,
        @JobParameterDefinition(
                type = "MULTILINE",
                defaultValue = "Line 1\nLine 2\nLine 3"
        )
        String notesExternalParameter,
        @JobParameterDefinition(
                defaultValue = "100"
        )
        int integerExternalParameter,
        @JobParameterDefinition(
                defaultValue = "3.14159"
        )
        double doubleExternalParameter,
        @JobParameterDefinition(
                defaultValue = "true"
        )
        boolean booleanExternalParameter,
        @JobParameterDefinition(
                defaultValue = "2026-01-01"
        )
        LocalDate dateExternalParameter,
        @JobParameterDefinition(
                defaultValue = "2026-01-01T12:00:00"
        )
        LocalDateTime dateTimeExternalParameter,
        @JobParameterDefinition(
                defaultValue = "OPTION_B"
        )
        EnumParameter enumExternalParameter,
        @JobParameterDefinition(
                defaultValue = "OPTION_A,OPTION_C"
        )
        EnumSet<EnumParameter> multiEnumExternalParameter
) {
}

package ch.css.jobrunr.control.jobs.externaldata;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSet;
import org.jobrunr.jobs.lambdas.JobRequest;

/**
 * Example batch job request using external parameter storage via @JobParameterSet.
 * <p>
 * This demonstrates how to use external parameters for batch jobs with potentially
 * large parameter sets. The parameters are stored in the database and only the
 * parameter set ID is passed to the job.
 * <p>
 * This job simulates a data processing workflow with configurable batch size and error simulation.
 */
public record ExternalDataBatchJobRequest(
        @JobParameterSet({
                @JobParameterDefinition(
                        name = "numberOfChildJobs",
                        type = "java.lang.Integer",
                        defaultValue = "100"
                ),
                @JobParameterDefinition(
                        name = "stringExternalParameter",
                        type = "java.lang.String",
                        defaultValue = "Default External String"
                ),
                @JobParameterDefinition(
                        name = "notesExternalParameter",
                        type = "MULTILINE",
                        defaultValue = "Line 1\nLine 2\nLine 3"
                ),
                @JobParameterDefinition(
                        name = "integerExternalParameter",
                        type = "java.lang.Integer",
                        defaultValue = "100"
                ),
                @JobParameterDefinition(
                        name = "booleanExternalParameter",
                        type = "java.lang.Boolean",
                        defaultValue = "true"
                ),
                @JobParameterDefinition(
                        name = "dateExternalParameter",
                        type = "java.time.LocalDate",
                        defaultValue = "2026-01-01"
                ),
                @JobParameterDefinition(
                        name = "dateTimeExternalParameter",
                        type = "java.time.LocalDateTime",
                        defaultValue = "2026-01-01T12:00:00"
                ),
                @JobParameterDefinition(
                        name = "enumExternalParameter",
                        type = "ch.css.jobrunr.control.jobs.parameters.EnumParameter",
                        defaultValue = "OPTION_B"
                ),
                @JobParameterDefinition(
                        name = "multiEnumExternalParameter",
                        type = "java.util.EnumSet<ch.css.jobrunr.control.jobs.parameters.EnumParameter>",
                        defaultValue = "OPTION_A,OPTION_C"
                )
        })
        String parameterSetId
) implements JobRequest {

    @Override
    public Class<ExternalDataBatchJob> getJobRequestHandler() {
        return ExternalDataBatchJob.class;
    }
}

package ch.css.jobrunr.control.jobs.batch.external;

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
 * This job simulates a data migration process with configurable batch size and error simulation.
 */
public record DataMigrationBatchJobRequest(
        @JobParameterSet({
                @JobParameterDefinition(
                        name = "sourceName",
                        type = "java.lang.String",
                        defaultValue = "legacy_system"
                ),
                @JobParameterDefinition(
                        name = "targetName",
                        type = "java.lang.String",
                        defaultValue = "new_system"
                ),
                @JobParameterDefinition(
                        name = "numberOfBatches",
                        type = "java.lang.Integer",
                        defaultValue = "50"
                ),
                @JobParameterDefinition(
                        name = "batchSize",
                        type = "java.lang.Integer",
                        defaultValue = "100"
                ),
                @JobParameterDefinition(
                        name = "simulateErrors",
                        type = "java.lang.Boolean",
                        defaultValue = "false"
                ),
                @JobParameterDefinition(
                        name = "migrationDate",
                        type = "java.time.LocalDate",
                        defaultValue = "2026-01-01"
                )
        })
        String parameterSetId
) implements JobRequest {

    @Override
    public Class<DataMigrationBatchJob> getJobRequestHandler() {
        return DataMigrationBatchJob.class;
    }
}

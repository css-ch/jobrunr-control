package ch.css.jobrunr.control.jobs.externaldata;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.jobs.parameters.EnumParameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Example batch job using external parameter storage.
 * <p>
 * This job demonstrates how to:
 * 1. Load parameters from external storage using the parameter set ID
 * 2. Process parameters and create batch items
 * 3. Enqueue background jobs for parallel processing
 * <p>
 * The job simulates a data processing workflow with external data sources.
 */
@ApplicationScoped
public class ExternalDataBatchJob implements JobRequestHandler<ExternalDataBatchJobRequest> {

    @Inject
    ParameterStorageService parameterStorageService;

    @ConfigurableJob(isBatch = true, labels = {"ExternalData"})
    @Transactional
    @Override
    public void run(ExternalDataBatchJobRequest request) {
        // Load parameters from external storage
        UUID parameterSetId = UUID.fromString(request.parameterSetId());
        ParameterSet parameterSet = parameterStorageService.findById(parameterSetId)
                .orElseThrow(() -> new IllegalStateException(
                        "Parameter set not found: " + parameterSetId));

        // Extract parameters (demonstrating all supported types)
        Integer numberOfChildJobs = parameterSet.getInteger("numberOfChildJobs");
        String stringParam = parameterSet.getString("stringExternalParameter");
        String notesParam = parameterSet.getString("notesExternalParameter");
        Integer integerParam = parameterSet.getInteger("integerExternalParameter");
        Boolean booleanParam = parameterSet.getBoolean("booleanExternalParameter");
        LocalDate dateParam = parameterSet.getDate("dateExternalParameter");
        LocalDateTime dateTimeParam = parameterSet.getDateTime("dateTimeExternalParameter");
        EnumParameter enumParam = parameterSet.getEnum("enumExternalParameter", EnumParameter.class);
        EnumSet<EnumParameter> multiEnumParam = parameterSet.getEnumSet("multiEnumExternalParameter", EnumParameter.class);

        jobContext().logger().info(String.format(
                "Starting external data batch job with all parameter types:%n" +
                        "  Number of Child Jobs: %d%n" +
                        "  String: %s%n" +
                        "  Notes (Multiline): %s%n" +
                        "  Integer: %d%n" +
                        "  Boolean: %b%n" +
                        "  LocalDate: %s%n" +
                        "  LocalDateTime: %s%n" +
                        "  Enum: %s%n" +
                        "  EnumSet: %s",
                numberOfChildJobs, stringParam, notesParam, integerParam, booleanParam, dateParam, dateTimeParam, enumParam, multiEnumParam));

        // Create batch items (using integer parameter for batch count)
        List<ExternalDataBatchItemRequest> items = IntStream.rangeClosed(1, numberOfChildJobs)
                .mapToObj(childId -> new ExternalDataBatchItemRequest(
                        childId,
                        stringParam,
                        booleanParam
                ))
                .toList();

        // Simulate preparation delay (e.g., connecting to source system)
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Save metadata
        jobContext().saveMetadata("numberOfChildJobs", numberOfChildJobs);
        jobContext().saveMetadata("stringParameter", stringParam);
        jobContext().saveMetadata("notesParameter", notesParam);
        jobContext().saveMetadata("integerParameter", integerParam);
        jobContext().saveMetadata("booleanParameter", booleanParam);
        jobContext().saveMetadata("dateParameter", dateParam.toString());
        jobContext().saveMetadata("dateTimeParameter", dateTimeParam.toString());
        jobContext().saveMetadata("enumParameter", enumParam.toString());
        jobContext().saveMetadata("multiEnumParameter", multiEnumParam.toString());

        jobContext().logger().info(String.format(
                "Enqueueing %d batch items for processing...", items.size()));

        // Enqueue background jobs for each batch item
        BackgroundJobRequest.enqueue(items.stream());
    }
}

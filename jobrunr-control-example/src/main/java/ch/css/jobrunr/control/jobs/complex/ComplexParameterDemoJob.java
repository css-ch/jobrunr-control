package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.DbBasedRecapAndMessages;
import ch.css.jobrunr.control.annotations.JobDetailPage;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.domain.details.JobMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@DbBasedRecapAndMessages
@ApplicationScoped
public class ComplexParameterDemoJob implements JobRequestHandler<ComplexParameterDemoJobRequest> {

    private static final Logger LOG = Logger.getLogger(ComplexParameterDemoJob.class);

    private static final String METADATA_KEY_ENQUEUED = "children_enqueued";

    private final ParameterStorageService parameterStorageService;
    private final JobMessageService messageService;

    @Inject
    public ComplexParameterDemoJob(ParameterStorageService parameterStorageService, JobMessageService messageService) {
        this.parameterStorageService = parameterStorageService;
        this.messageService = messageService;
    }

    @Override
    @ConfigurableJob(name = "Complex Demo Job", isBatch = true, retries = 0)
    @JobDetailPage(
            recapParameterClass = ComplexParameterDemoJobRecap.class, showEmptyParameters = false, showRecapParameterWithZeroValue = false,
            messageProviderKey = "db-based-job-details-provider", recapProviderKey = "db-based-job-details-provider"
    )
    public void run(ComplexParameterDemoJobRequest complexParameterDemoJobRequest) throws Exception {
        final UUID jobId = ThreadLocalJobContext.getJobContext().getJobId();

        if (isAlreadyEnqueued()) {
            messageService.info("Child jobs already enqueued, skipping to prevent duplicates");
            return;
        }

        messageService.info("Start ComplexParameterDemoJob");
        LOG.debug(String.format("[Batch %s] Start ComplexParameterDemoJob", jobId.toString()));

        ComplexParameterDemoJobParameter parameter = parameterStorageService.findById(jobId, ComplexParameterDemoJobParameter.class)
                .orElseThrow(() -> new IllegalStateException("Parameter set not found: " + jobId));

        int childCount = 87;
        try {
            childCount = Integer.parseInt(parameter.steuerungBeilageNrs());
        } catch (NumberFormatException e) {
            messageService.warning("Invalid number format for steuerungBeilageNrs: " + parameter.steuerungBeilageNrs());
        }
        List<ComplexParameterDemoChildJobRequest> items = IntStream.range(1, childCount).boxed()
                .map((Integer number) -> new ComplexParameterDemoChildJobRequest(number, number == 13 && parameter.steuerungPhysischerDruckPortalVersand()))
                .toList();

        markAsEnqueued(items.size());
        enqueueChildJobs(items);
        LOG.info(String.format("[Batch %s] Preparing ComplexParameterDemoJob finished. %d Child-Jobs enqueued", jobId, items.size()));
        messageService.info(String.format("Preparing ComplexParameterDemoJob finished. %d Child-Jobs enqueued", items.size()));
    }

    private boolean isAlreadyEnqueued() {
        var metadata = ThreadLocalJobContext.getJobContext().getMetadata();
        return metadata.containsKey(METADATA_KEY_ENQUEUED) &&
                Boolean.TRUE.equals(metadata.get(METADATA_KEY_ENQUEUED));
    }

    private void markAsEnqueued(int itemCount) {
        JobContext jobContext = ThreadLocalJobContext.getJobContext();
        jobContext.saveMetadata(METADATA_KEY_ENQUEUED, true);
        jobContext.saveMetadata("total_children", itemCount);
        jobContext.saveMetadata("enqueued_at", java.time.Instant.now().toString());
    }

    private void enqueueChildJobs(List<ComplexParameterDemoChildJobRequest> items) {
        // Enqueue all items in a stream (JobRunr handles the scheduling)
        items.forEach(item -> BackgroundJobRequest.create(aJob()
                .withJobRequest(item)
                .withAmountOfRetries(0)
                .withName("Complex Parameter Child-Worker Nr: " + item.number())));
        messageService.info(String.format("Enqueued %d child jobs (retry-safe via metadata check)", items.size()));
    }
}

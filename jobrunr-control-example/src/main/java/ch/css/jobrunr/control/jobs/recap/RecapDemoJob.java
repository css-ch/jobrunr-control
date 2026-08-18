package ch.css.jobrunr.control.jobs.recap;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.DbBasedRecapAndMessages;
import ch.css.jobrunr.control.annotations.JobDetailPage;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.domain.details.JobMessageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.jobrunr.scheduling.JobBuilder.aBatchJob;
import static org.jobrunr.scheduling.JobBuilder.aJob;

/**
 * Demonstrates database-backed recap aggregation for workers nested below a batch wrapper.
 */
@DbBasedRecapAndMessages
@ApplicationScoped
public class RecapDemoJob implements JobRequestHandler<RecapDemoJobRequest> {

    private static final Logger LOG = Logger.getLogger(RecapDemoJob.class);
    private static final String ENQUEUE_WORKFLOW_STEP = "enqueue-workflow";

    private final ParameterStorageService parameterStorageService;
    private final JobMessageService messageService;

    @Inject
    public RecapDemoJob(ParameterStorageService parameterStorageService, JobMessageService messageService) {
        this.parameterStorageService = parameterStorageService;
        this.messageService = messageService;
    }

    @Override
    @ConfigurableJob(name = "Recap Demo Job", isBatch = true, retries = 0)
    @Transactional
    @JobDetailPage(
            recapParameterClass = RecapDemoCounters.class,
            showEmptyParameters = false,
            showRecapParameterWithZeroValue = false,
            messageProviderKey = "db-based-job-details-provider",
            recapProviderKey = "db-based-job-details-provider"
    )
    public void run(RecapDemoJobRequest request) {
        JobContext jobContext = ThreadLocalJobContext.getJobContext();
        UUID workflowRootJobId = jobContext.getJobId();

        if (jobContext.hasCompletedStep(ENQUEUE_WORKFLOW_STEP)) {
            messageService.info("Recap workflow already enqueued, skipping completed durable step");
            return;
        }

        messageService.info("Start RecapDemoJob");
        LOG.debugf("[Batch %s] Start RecapDemoJob", workflowRootJobId);

        RecapDemoJobParameter parameter = parameterStorageService
                .findById(workflowRootJobId, RecapDemoJobParameter.class)
                .orElseThrow(() -> new IllegalStateException("Parameter set not found: " + workflowRootJobId));

        int workerCount = resolveWorkerCount(parameter);
        int configuredWorkerCount = workerCount > 1 ? workerCount - 1 : 0;
        String rootJobName = jobContext.getJobName();

        jobContext.runStepOnce(ENQUEUE_WORKFLOW_STEP, () -> {
            enqueueWorkerBatch(workflowRootJobId, rootJobName, workerCount,
                    parameter.steuerungPhysischerDruckPortalVersand());
            saveEnqueueMetadata(configuredWorkerCount);
        });

        LOG.infof("[Batch %s] Preparing RecapDemoJob finished. %d workers configured",
                workflowRootJobId, configuredWorkerCount);
        messageService.info(String.format(
                "Preparing RecapDemoJob finished. %d workers configured", configuredWorkerCount));
    }

    private int resolveWorkerCount(RecapDemoJobParameter parameter) {
        int workerCount = 87;
        try {
            workerCount = Integer.parseInt(parameter.steuerungBeilageNrs());
        } catch (NumberFormatException e) {
            messageService.warning("Invalid number format for steuerungBeilageNrs: "
                    + parameter.steuerungBeilageNrs());
        }

        return workerCount;
    }

    private void enqueueWorkerBatch(UUID workflowRootJobId, String rootJobName,
                                    int workerCount, Boolean forcePhysicalPrintForPortalDelivery) {
        BackgroundJob.create(aBatchJob()
                .withId(deterministicJobId(workflowRootJobId, "worker-batch"))
                .withJobLambda(() -> enqueueWorkers(workflowRootJobId, rootJobName, workerCount,
                        forcePhysicalPrintForPortalDelivery))
                .withAmountOfRetries(0)
                .withName(rootJobName + "-WorkerBatch"));
    }

    /**
     * Public because JobRunr invokes the analyzed lambda target reflectively.
     */
    public void enqueueWorkers(UUID workflowRootJobId, String rootJobName,
                               int workerCount, Boolean forcePhysicalPrintForPortalDelivery) {
        List<RecapDemoWorkerRequest> workers = IntStream.range(1, workerCount)
                .mapToObj(number -> new RecapDemoWorkerRequest(
                        number,
                        number == 13 && forcePhysicalPrintForPortalDelivery))
                .toList();

        workers.forEach(worker -> BackgroundJobRequest.create(aJob()
                .withId(deterministicJobId(workflowRootJobId, "worker-" + worker.number()))
                .withJobRequest(worker)
                .withAmountOfRetries(0)
                .withName(String.format("%s-Worker-%d", rootJobName, worker.number()))));

        messageService.info(String.format(
                "Enqueued %d workers (retry-safe via durable step and deterministic IDs)", workers.size()));
    }

    private void saveEnqueueMetadata(int workerCount) {
        JobContext jobContext = ThreadLocalJobContext.getJobContext();
        jobContext.saveMetadata("workers_enqueued", true);
        jobContext.saveMetadata("total_workers", workerCount);
        jobContext.saveMetadata("enqueued_at", Instant.now().toString());
    }

    private static UUID deterministicJobId(UUID workflowRootJobId, String role) {
        return UUID.nameUUIDFromBytes((workflowRootJobId + ":" + role).getBytes(StandardCharsets.UTF_8));
    }
}

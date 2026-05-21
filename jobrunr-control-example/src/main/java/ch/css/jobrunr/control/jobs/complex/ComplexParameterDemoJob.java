package ch.css.jobrunr.control.jobs.complex;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.jobrunr.scheduling.JobBuilder.aJob;

@ApplicationScoped
public class ComplexParameterDemoJob implements JobRequestHandler<ComplexParameterDemoJobRequest> {

    private static final Logger LOG = Logger.getLogger(ComplexParameterDemoJob.class);

    private static final String METADATA_KEY_ENQUEUED = "children_enqueued";

    @Override
    @ConfigurableJob(name="Complex Demo Job", isBatch = true, resultPageUrl = "http://{host}:{port}/mybatch/result/{jobId}?stage={stage}&startDate={startDate}&endDate={endDate}")
    public void run(ComplexParameterDemoJobRequest complexParameterDemoJobRequest) throws Exception {
        final UUID jobId = ThreadLocalJobContext.getJobContext().getJobId();

        if (isAlreadyEnqueued()) {
            ThreadLocalJobContext.getJobContext().logger().info("Child jobs already enqueued, skipping to prevent duplicates");
            return;
        }

        ThreadLocalJobContext.getJobContext().logger().info("Start ComplexParameterDemoJob");
        LOG.debug(String.format("[Batch %s] Start ComplexParameterDemoJob", jobId.toString()));

        List<ComplexParameterDemoChildJobRequest> items = IntStream.range(1, 87).boxed()
                .map(ComplexParameterDemoChildJobRequest::new)
                .toList();

        markAsEnqueued(items.size());
        enqueueChildJobs(items);
        LOG.info(String.format("[Batch %s] Preparing ComplexParameterDemoJob finished. %d Child-Jobs enqueued", jobId, items.size()));
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
                .withName("Child-Worker Nr: " + item.number())));
        ThreadLocalJobContext.getJobContext().logger().info(String.format("Enqueued %d child jobs (retry-safe via metadata check)", items.size()));
    }
}

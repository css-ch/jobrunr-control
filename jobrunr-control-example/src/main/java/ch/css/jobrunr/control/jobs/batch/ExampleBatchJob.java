package ch.css.jobrunr.control.jobs.batch;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.jobrunr.scheduling.BackgroundJobRequest;

import java.util.List;
import java.util.stream.IntStream;

@ApplicationScoped
public class ExampleBatchJob implements JobRequestHandler<ExampleBatchJobRequest> {

    @ConfigurableJob(isBatch = true, labels = {"Example", "Batch"}, deleteOnSuccess = "PT5M!PT10H")
    @Override
    public void run(ExampleBatchJobRequest request) throws Exception {
        jobContext().logger().info(String.format("Preparing batch job with numberOfJunks: %d, junkSize: %d, simulateErrors: %b",
                request.numberOfJunks(), request.junkSize(), request.simulateErrors()));
        // Load all items to be processed based on request parameters
        List<ExampleBatchJobItemProcessorRequest> items = IntStream.rangeClosed(1, request.numberOfJunks())
                .mapToObj(junkId -> new ExampleBatchJobItemProcessorRequest(junkId, request.junkSize(), request.simulateErrors()))
                .toList();

        // Simulate preparation delay
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        jobContext().saveMetadata("hello", "ExampleBatchJob#run");
        // Enqueue background jobs for each item
        BackgroundJobRequest.enqueue(items.stream());
    }
}

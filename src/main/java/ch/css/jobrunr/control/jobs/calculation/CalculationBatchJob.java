package ch.css.jobrunr.control.jobs.calculation;

import ch.css.jobrunr.control.infrastructure.discovery.ConfigurableJob;
import ch.css.jobrunr.control.infrastructure.discovery.annotation.BatchJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.scheduling.BackgroundJobRequest;

import java.util.List;
import java.util.stream.IntStream;

@ApplicationScoped
public class CalculationBatchJob implements ConfigurableJob<CalculationBatchJobRequest> {

    @BatchJob
    @Override
    public void run(CalculationBatchJobRequest request) throws Exception {
        jobContext().logger().info(String.format("Preparing batch job with totalItems: %d, batchSize: %d, simulateErrors: %b",
                request.totalItems(), request.batchSize(), request.simulateErrors()));
        // Load all items to be processed based on request parameters
        List<CalculationItemProcessorRequest> items = IntStream.rangeClosed(1, request.totalItems())
                .mapToObj(x -> new CalculationItemProcessorRequest(x, request.simulateErrors()))
                .toList();

        // Simulate preparation delay
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Enqueue background jobs for each item
        BackgroundJobRequest.enqueue(items.stream());
    }
}

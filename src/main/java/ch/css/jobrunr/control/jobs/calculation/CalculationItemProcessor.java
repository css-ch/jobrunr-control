package ch.css.jobrunr.control.jobs.calculation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Random;

@ApplicationScoped
public class CalculationItemProcessor implements JobRequestHandler<CalculationItemProcessorRequest> {

    private static final Random random = new Random();

    @Override
    @Transactional
    @Job(name = "Processing calculation item: %0", retries = 0)
    public void run(CalculationItemProcessorRequest request) throws Exception {
        jobContext().logger().info(String.format("Processing item: %s", request));
        // Simulate processing delay
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (request.simulateErrors()) {
            if (random.nextDouble() < 0.01) {
                jobContext().logger().error("Simulated error processing item: " + request.itemToCompute());
                throw new Exception("Simulated error processing item: " + request.itemToCompute());
            }
        }
        jobContext().logger().info("Processing done.");
    }
}

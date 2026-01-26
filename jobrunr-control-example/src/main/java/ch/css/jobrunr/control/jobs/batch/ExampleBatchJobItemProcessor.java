package ch.css.jobrunr.control.jobs.batch;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.Random;

@ApplicationScoped
public class ExampleBatchJobItemProcessor implements JobRequestHandler<ExampleBatchJobItemProcessorRequest> {

    private static final Random random = new Random();

    @Override
    @Transactional
    @Job(name = "Processing example batch junkId: %0", retries = 0)
    public void run(ExampleBatchJobItemProcessorRequest request) throws Exception {
        jobContext().logger().info(String.format("Processing junkId: %s", request.junkId()));
        for (int i = 0; i < request.junkSize(); i++) {
            jobContext().logger().info(String.format("\tProcessing item in junk: %s", i));
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (request.simulateErrors()) {
                if (random.nextDouble() < (0.02 / request.junkSize())) {
                    jobContext().logger().error("Simulated error processing item: " + request.junkId());
                    throw new Exception("Simulated error processing item: " + request.junkId());
                }
            }
        }
        jobContext().logger().info("Processing done.");
    }
}

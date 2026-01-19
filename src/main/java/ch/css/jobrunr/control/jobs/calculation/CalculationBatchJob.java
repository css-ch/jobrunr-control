package ch.css.jobrunr.control.jobs.calculation;

import ch.css.jobrunr.control.infrastructure.discovery.ConfigurableJob;
import jakarta.enterprise.context.ApplicationScoped;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.BackgroundJobRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch job with progress logging.
 *
 * <p>
 * Demonstrates batch processing by enqueuing background jobs for each user and
 * shows how to simulate errors for testing retries and failure handling.
 * </p>
 */

@ApplicationScoped
public class CalculationBatchJob implements ConfigurableJob<CalculationBatchJobRequest> {

    private static final Logger log = LoggerFactory.getLogger(CalculationBatchJob.class);
    
    /**
     * Starts the batch job by preparing and enqueuing child jobs.
     */
    @Override
    @Job(name = "CalculationBatchJob", retries = 2)
    public void run(CalculationBatchJobRequest request) throws Exception {
        CalculationBatchJobPreparerRequest preparerRequest =
                new CalculationBatchJobPreparerRequest(
                        request.totalItems(),
                        request.batchSize(),
                        request.simulateErrors()
                );
        BackgroundJobRequest.startBatch(preparerRequest);
    }
}

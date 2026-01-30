package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.exceptions.JobExecutionException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.states.AbstractInitialJobState;
import org.jobrunr.jobs.states.EnqueuedState;
import org.jobrunr.jobs.states.StateName;
import org.jobrunr.storage.JobSearchRequest;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.navigation.AmountRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory for creating JobRunr JobSearchRequest instances.
 * Provides common search request configurations for different job types.
 */
@ApplicationScoped
public class ConfigurableJobSearchAdapter {

    public record ConfigurableJobSearchResult(JobDefinition jobDefinition, Job job) {
    }

    private static final Logger log = Logger.getLogger(ConfigurableJobSearchAdapter.class);

    private final StorageProvider storageProvider;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public ConfigurableJobSearchAdapter(
            StorageProvider storageProvider,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService
    ) {
        this.storageProvider = storageProvider;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    public List<ConfigurableJobSearchResult> getConfigurableJob(List<StateName> statesToQuery) {
        List<ConfigurableJobSearchResult> configurableJob = new ArrayList<>();
        try {
            AmountRequest amountRequest = new AmountRequest("updatedAt:DESC", 10000);

            for (StateName state : statesToQuery) {
                for (JobDefinition jobDefinition : jobDefinitionDiscoveryService.getAllJobDefinitions()) {
                    try {
                        JobSearchRequest searchRequest;
                        if (jobDefinition.isBatchJob()) {
                            searchRequest = ConfigurableJobSearchAdapter.createSearchRequestForStateAndJobTypeForBatch(state, jobDefinition.jobType());
                        } else {
                            searchRequest = ConfigurableJobSearchAdapter.createSearchRequestForStateAndJobType(state, jobDefinition.jobType());
                        }

                        List<Job> jobList = storageProvider.getJobList(searchRequest, amountRequest);
                        for (Job job : jobList) {
                            if (!isChildJobOfBatch(job, jobDefinition)) { // Check is necessary because JobRunr copies labels to child jobs
                                configurableJob.add(new ConfigurableJobSearchResult(jobDefinition, job));
                            }
                        }
                    } catch (Exception e) {
                        log.warnf("Error retrieving jobs in state %s with type %s: %s", state, jobDefinition.jobType(), e.getMessage());
                    }
                }
            }
            return configurableJob;
        } catch (Exception e) {
            log.errorf("Error retrieving job executions", e);
            throw new JobExecutionException("Error retrieving job executions", e);
        }
    }

    private boolean isChildJobOfBatch(Job job, JobDefinition jobDefinition) {
        return jobDefinition.isBatchJob() && getParentJob(job) != null;
    }

    private UUID getParentJob(Job job) {
        return job.getJobStatesOfType(EnqueuedState.class).findFirst().map(AbstractInitialJobState::getParentJobId).orElse(null);
    }

    /**
     * Creates a JobSearchRequest for a specific state and job type.
     *
     * @param state   the job state to search for
     * @param jobType the job type label (e.g., simple class name)
     * @return a configured JobSearchRequest
     */
    public static JobSearchRequest createSearchRequestForStateAndJobType(StateName state, String jobType) {
        return new JobSearchRequest(
                state,                     // state
                null,                      // priority
                null,                      // jobId
                null,                      // jobIdGreaterThan
                null,                      // jobIds
                null,                      // jobName
                null,                      // jobSignature
                null,                      // jobExceptionType
                null,                      // jobFingerprint
                "jobtype:" + jobType,      // label
                null,                      // serverTag
                null,                      // mutex
                null,                      // recurringJobId
                null,                      // recurringJobIds
                null,                      // awaitingOn
                null,                      // parentId
                null,                      // rateLimiter
                null,                      // onlyBatchJobs
                null,                      // createdAtFrom
                null,                      // createdAtTo
                null,                      // updatedAtFrom
                null,                      // updatedAtTo
                null,                      // scheduledAtFrom
                null,                      // scheduledAtTo
                null                       // deleteAtTo
        );
    }

    /**
     * Creates a JobSearchRequest for a specific state and batch job type.
     * This variant sets onlyBatchJobs to true to filter for batch jobs only.
     *
     * @param state   the job state to search for
     * @param jobType the job type label (e.g., simple class name)
     * @return a configured JobSearchRequest for batch jobs
     */
    public static JobSearchRequest createSearchRequestForStateAndJobTypeForBatch(StateName state, String jobType) {
        return new JobSearchRequest(
                state,                     // state
                null,                      // priority
                null,                      // jobId
                null,                      // jobIdGreaterThan
                null,                      // jobIds
                null,                      // jobName
                null,                      // jobSignature
                null,                      // jobExceptionType
                null,                      // jobFingerprint
                "jobtype:" + jobType,      // label
                null,                      // serverTag
                null,                      // mutex
                null,                      // recurringJobId
                null,                      // recurringJobIds
                null,                      // awaitingOn
                null,                      // parentId
                null,                      // rateLimiter
                true,                      // onlyBatchJobs
                null,                      // createdAtFrom
                null,                      // createdAtTo
                null,                      // updatedAtFrom
                null,                      // updatedAtTo
                null,                      // scheduledAtFrom
                null,                      // scheduledAtTo
                null                       // deleteAtTo
        );
    }
}

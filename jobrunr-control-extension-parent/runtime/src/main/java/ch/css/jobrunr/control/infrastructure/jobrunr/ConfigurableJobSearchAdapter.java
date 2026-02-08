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
import org.jobrunr.storage.JobSearchRequestBuilder;
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

    private static final Logger LOG = Logger.getLogger(ConfigurableJobSearchAdapter.class);

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
            AmountRequest amountRequest = createAmountRequest();

            for (StateName state : statesToQuery) {
                collectJobsForState(state, amountRequest, configurableJob);
            }
            return configurableJob;
        } catch (Exception e) {
            LOG.errorf(e, "Error retrieving job executions");
            throw new JobExecutionException("Error retrieving job executions", e);
        }
    }

    private AmountRequest createAmountRequest() {
        return new AmountRequest("updatedAt:DESC", 10000);
    }

    private void collectJobsForState(StateName state, AmountRequest amountRequest, List<ConfigurableJobSearchResult> results) {
        for (JobDefinition jobDefinition : jobDefinitionDiscoveryService.getAllJobDefinitions()) {
            collectJobsForDefinition(state, jobDefinition, amountRequest, results);
        }
    }

    private void collectJobsForDefinition(StateName state, JobDefinition jobDefinition, AmountRequest amountRequest, List<ConfigurableJobSearchResult> results) {
        try {
            JobSearchRequest searchRequest = createSearchRequest(state, jobDefinition);
            List<Job> jobList = storageProvider.getJobList(searchRequest, amountRequest);
            addNonChildJobs(jobList, jobDefinition, results);
        } catch (Exception e) {
            LOG.warnf(e, "Error retrieving jobs in state %s with type %s", state, jobDefinition.jobType());
        }
    }

    private JobSearchRequest createSearchRequest(StateName state, JobDefinition jobDefinition) {
        if (jobDefinition.isBatchJob()) {
            return createSearchRequestForStateAndJobTypeForBatch(state, jobDefinition.jobType());
        } else {
            return createSearchRequestForStateAndJobType(state, jobDefinition.jobType());
        }
    }

    private void addNonChildJobs(List<Job> jobList, JobDefinition jobDefinition, List<ConfigurableJobSearchResult> results) {
        for (Job job : jobList) {
            if (!isChildJobOfBatch(job, jobDefinition)) {
                results.add(new ConfigurableJobSearchResult(jobDefinition, job));
            }
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
        return JobSearchRequestBuilder
                .aJobSearchRequest()
                .withStateName(state)
                .withLabel("jobtype:" + jobType)
                .build();
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
        return JobSearchRequestBuilder
                .aJobSearchRequest()
                .withOnlyBatchJobs(true)
                .withStateName(state)
                .withLabel("jobtype:" + jobType).build();
    }
}

package ch.css.jobrunr.control.application.details;

import ch.css.jobrunr.control.domain.*;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Retrieves job execution parameters for the dashboard.
 */
@ApplicationScoped
public class GetJobDetailsParametersUseCase {

    private static final Logger LOG = Logger.getLogger(GetJobDetailsParametersUseCase.class);

    private final JobExecutionPort jobExecutionPort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public GetJobDetailsParametersUseCase(JobExecutionPort jobExecutionPort, JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobExecutionPort = jobExecutionPort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    /**
     * Retrieves the parameters for a job execution.
     *
     * @param jobId Job ID (UUID as String)
     * @return Result containing job execution parameters
     * @throws JobNotFoundException when the job execution is not found
     * @throws IllegalArgumentException when jobId is invalid or null
     */
    public Result execute(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            LOG.errorf("Job ID must not be null or empty");
            throw new IllegalArgumentException("Job ID must not be null or empty");
        }

        UUID jobUuid;
        try {
            jobUuid = UUID.fromString(jobId);
        } catch (IllegalArgumentException e) {
            LOG.errorf(e, "Invalid job ID format: %s", jobId);
            throw new IllegalArgumentException("Invalid job ID format: " + jobId, e);
        }

        JobExecutionInfo execution = jobExecutionPort.getJobExecutionById(jobUuid)
                .orElseThrow(() -> {
                    LOG.errorf("Job execution not found: %s", jobId);
                    return new JobNotFoundException("Job execution with ID " + jobId + " not found");
                });

        // Convert parameters map to maintain order and consistent display
        Map<String, Object> parameters = new LinkedHashMap<>(execution.getParameters());

        JobDefinition jobDefinition = jobDefinitionDiscoveryService.requireJobByType(execution.jobType());
        boolean showEmptyParameters = jobDefinition.jobDetailPage() != null && jobDefinition.jobDetailPage().showEmptyParameters();

        LOG.infof("Retrieved %d parameters for job %s", parameters.size(), jobId);

        return new Result(parameters, jobDefinition.parameterSections(), jobDefinition.parameters(), showEmptyParameters);
    }

    public record Result(
            Map<String, Object> parameters,
            List<JobParameterSection> parameterSections,
            List<JobParameter> parameterDefinitions,
            boolean showEmptyParameters) {
    }
}


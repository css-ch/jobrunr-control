package ch.css.jobrunr.control.application.discovery;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

/**
 * Use Case: Returns the parameter schema for a selected job.
 */
@ApplicationScoped
public class GetJobParametersUseCase {

    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public GetJobParametersUseCase(JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    /**
     * Returns the parameter definition for a job.
     *
     * @param jobType Job type
     * @return List of job parameters
     * @throws JobNotFoundException when the job is not found
     */
    public List<JobParameter> execute(String jobType) {
        Optional<JobDefinition> jobDefinition = jobDefinitionDiscoveryService.findJobByType(jobType);

        if (jobDefinition.isEmpty()) {
            throw new JobNotFoundException("JobDefinition with JobType '" + jobType + "' not found");
        }

        return jobDefinition.get().parameters();
    }
}


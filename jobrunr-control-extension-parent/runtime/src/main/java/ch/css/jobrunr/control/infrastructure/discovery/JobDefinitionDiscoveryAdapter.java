package ch.css.jobrunr.control.infrastructure.discovery;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * JobRunr-based implementation of JobDefinitionDiscoveryService.
 */
@ApplicationScoped
public class JobDefinitionDiscoveryAdapter implements JobDefinitionDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(JobDefinitionDiscoveryAdapter.class);

    /**
     * Returns all discovered job definitions.
     *
     * @return list of job definitions
     */
    @Override
    public Collection<JobDefinition> getAllJobDefinitions() {
        return JobDefinitionRecorder.JobDefinitionRegistry.INSTANCE.getAllDefinitions();
    }

    /**
     * Finds a job definition by its type.
     *
     * @param jobType the job type
     * @return optional job definition
     */
    @Override
    public Optional<JobDefinition> findJobByType(String jobType) {
        return getAllJobDefinitions().stream()
                .filter(jd -> Objects.equals(jd.jobType(), jobType))
                .findFirst();
    }
}



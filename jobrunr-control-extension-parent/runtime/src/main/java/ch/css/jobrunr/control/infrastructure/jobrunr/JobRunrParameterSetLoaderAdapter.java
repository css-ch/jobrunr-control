package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobDefinitionDiscoveryService;
import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterSetLoaderPort;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import ch.css.jobrunr.control.domain.exceptions.ParameterSetNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.storage.StorageProvider;

import java.util.Map;
import java.util.UUID;

/**
 * Loads parameters from either JobRunr storage or external parameter repository.
 * Determines whether to use external storage based on the job type definition.
 */
@ApplicationScoped
public class JobRunrParameterSetLoaderAdapter implements ParameterSetLoaderPort {

    private static final Logger LOG = Logger.getLogger(JobRunrParameterSetLoaderAdapter.class);

    private final StorageProvider storageProvider;
    private final ParameterStoragePort parameterStoragePort;
    private final JobDefinitionDiscoveryService jobDefinitionDiscoveryService;

    @Inject
    public JobRunrParameterSetLoaderAdapter(
            StorageProvider storageProvider,
            ParameterStoragePort parameterStoragePort,
            JobDefinitionDiscoveryService jobDefinitionDiscoveryService) {
        this.storageProvider = storageProvider;
        this.parameterStoragePort = parameterStoragePort;
        this.jobDefinitionDiscoveryService = jobDefinitionDiscoveryService;
    }

    @Override
    public Map<String, Object> loadParameters(UUID jobId) {
        var job = storageProvider.getJobById(jobId);

        String handlerClassName = job.getJobDetails().getClassName();
        String simpleClassName = handlerClassName.substring(handlerClassName.lastIndexOf('.') + 1);

        boolean usesExternalParameters = jobDefinitionDiscoveryService
                .findJobByType(simpleClassName)
                .map(JobDefinition::usesExternalParameters)
                .orElse(false);

        if (usesExternalParameters) {
            LOG.debugf("Loading external parameters for job %s using job ID as parameter set ID", jobId);
            return loadParametersBySetId(jobId);
        }

        return JobParameterExtractor.extractParameters(job);
    }

    @Override
    public Map<String, Object> loadParametersBySetId(UUID parameterSetId) {
        return parameterStoragePort.findById(parameterSetId)
                .map(ParameterSet::parameters)
                .orElseThrow(() -> new ParameterSetNotFoundException(parameterSetId));
    }
}

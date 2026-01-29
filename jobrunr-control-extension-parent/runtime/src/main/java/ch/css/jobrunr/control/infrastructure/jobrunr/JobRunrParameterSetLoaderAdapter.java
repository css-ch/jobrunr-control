package ch.css.jobrunr.control.infrastructure.jobrunr;

import ch.css.jobrunr.control.domain.ParameterSetLoaderPort;
import ch.css.jobrunr.control.domain.ParameterSetNotFoundException;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.jobrunr.storage.StorageProvider;

import java.util.Map;
import java.util.UUID;

/**
 * Loads parameters from either JobRunr storage or external parameter repository.
 */
@ApplicationScoped
public class JobRunrParameterSetLoaderAdapter implements ParameterSetLoaderPort {

    private static final Logger log = Logger.getLogger(JobRunrParameterSetLoaderAdapter.class);

    private final StorageProvider storageProvider;
    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public JobRunrParameterSetLoaderAdapter(
            StorageProvider storageProvider,
            ParameterStoragePort parameterStoragePort) {
        this.storageProvider = storageProvider;
        this.parameterStoragePort = parameterStoragePort;
    }

    @Override
    public Map<String, Object> loadParameters(UUID jobId) {
        var job = storageProvider.getJobById(jobId);
        Map<String, Object> parameters = JobParameterExtractor.extractParameters(job);

        // Check if parameters contain reference to external storage
        if (parameters.containsKey("__parameterSetId")) {
            String paramSetIdStr = (String) parameters.get("__parameterSetId");
            UUID parameterSetId = UUID.fromString(paramSetIdStr);
            log.debugf("Loading external parameters for job %s from parameter set %s", jobId, parameterSetId);
            return loadParametersBySetId(parameterSetId);
        }

        return parameters;
    }

    @Override
    public Map<String, Object> loadParametersBySetId(UUID parameterSetId) {
        return parameterStoragePort.findById(parameterSetId)
                .map(paramSet -> {
                    // Update last accessed timestamp
                    parameterStoragePort.updateLastAccessed(parameterSetId);
                    return paramSet.parameters();
                })
                .orElseThrow(() -> new ParameterSetNotFoundException(parameterSetId));
    }
}

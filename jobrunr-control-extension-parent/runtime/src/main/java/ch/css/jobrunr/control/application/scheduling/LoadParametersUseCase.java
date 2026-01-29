package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.ParameterSetLoaderPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for loading job parameters.
 */
@ApplicationScoped
public class LoadParametersUseCase {

    private final ParameterSetLoaderPort loaderPort;

    @Inject
    public LoadParametersUseCase(ParameterSetLoaderPort loaderPort) {
        this.loaderPort = loaderPort;
    }

    public Map<String, Object> execute(UUID jobId) {
        return loaderPort.loadParameters(jobId);
    }

    public Map<String, Object> executeBySetId(UUID parameterSetId) {
        return loaderPort.loadParametersBySetId(parameterSetId);
    }
}

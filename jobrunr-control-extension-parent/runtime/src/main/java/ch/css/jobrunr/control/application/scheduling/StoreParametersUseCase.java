package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

/**
 * Use case for storing job parameters externally.
 *
 * @deprecated This use case is deprecated. Parameter storage is now handled automatically
 * in {@link CreateScheduledJobUseCase} and {@link UpdateScheduledJobUseCase}
 * based on the @JobParameterSet annotation. Use {@link ParameterStorageService} directly
 * if you need to store parameters outside of job scheduling.
 */
@Deprecated(since = "1.1.0", forRemoval = true)
@ApplicationScoped
public class StoreParametersUseCase {

    private static final Logger log = Logger.getLogger(StoreParametersUseCase.class);

    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public StoreParametersUseCase(ParameterStoragePort parameterStoragePort) {
        this.parameterStoragePort = parameterStoragePort;
    }

    /**
     * Stores parameters externally.
     *
     * @param jobType    the job type
     * @param parameters the parameters to store
     * @return the parameter set ID
     * @deprecated Use {@link ParameterStorageService#store(ParameterSet)} directly
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public UUID execute(String jobType, Map<String, Object> parameters) {

        UUID parameterSetId = UUID.randomUUID();
        ParameterSet parameterSet = ParameterSet.create(parameterSetId, jobType, parameters);

        parameterStoragePort.store(parameterSet);
        log.infof("Stored parameter set %s for jobType: %s", parameterSetId, jobType);

        return parameterSetId;
    }
}

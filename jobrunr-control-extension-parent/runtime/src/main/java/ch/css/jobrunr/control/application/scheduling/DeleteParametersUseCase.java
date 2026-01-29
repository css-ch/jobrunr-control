package ch.css.jobrunr.control.application.scheduling;

import ch.css.jobrunr.control.domain.ParameterStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Use case for deleting externally stored parameters.
 */
@ApplicationScoped
public class DeleteParametersUseCase {

    private static final Logger log = Logger.getLogger(DeleteParametersUseCase.class);

    private final ParameterStoragePort parameterStoragePort;

    @Inject
    public DeleteParametersUseCase(ParameterStoragePort parameterStoragePort) {
        this.parameterStoragePort = parameterStoragePort;
    }

    /**
     * Deletes a parameter set if it exists.
     *
     * @param parameterSetId the parameter set ID to delete
     */
    public void execute(UUID parameterSetId) {
        if (parameterSetId == null) {
            return;
        }

        parameterStoragePort.deleteById(parameterSetId);
        log.infof("Deleted parameter set: %s", parameterSetId);
    }
}

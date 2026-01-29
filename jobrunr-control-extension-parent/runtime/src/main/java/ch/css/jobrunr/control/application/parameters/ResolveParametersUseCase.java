package ch.css.jobrunr.control.application.parameters;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Resolves job parameters by expanding external parameter set references.
 * <p>
 * When a job uses external parameter storage (@JobParameterSet), the parameters map
 * contains a "__parameterSetId" key. This use case resolves that ID and returns
 * the actual parameters for display purposes.
 */
@ApplicationScoped
public class ResolveParametersUseCase {

    private static final Logger log = Logger.getLogger(ResolveParametersUseCase.class);
    private static final String PARAMETER_SET_ID_KEY = "__parameterSetId";

    private final ParameterStorageService parameterStorageService;

    @Inject
    public ResolveParametersUseCase(ParameterStorageService parameterStorageService) {
        this.parameterStorageService = parameterStorageService;
    }

    /**
     * Resolves parameters by expanding external parameter set references.
     * If the parameters contain a "__parameterSetId" key, it loads the parameter set
     * from external storage and returns the actual parameters.
     *
     * @param parameters the raw parameters map (may contain __parameterSetId)
     * @return the resolved parameters (with external parameters expanded)
     */
    public Map<String, Object> execute(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return new HashMap<>();
        }

        // Check if this uses external parameter storage
        if (!parameters.containsKey(PARAMETER_SET_ID_KEY)) {
            // Inline parameters - return as is
            return new HashMap<>(parameters);
        }

        // External parameters - resolve from storage
        String paramSetIdStr = (String) parameters.get(PARAMETER_SET_ID_KEY);
        try {
            UUID paramSetId = UUID.fromString(paramSetIdStr);
            return parameterStorageService.findById(paramSetId)
                    .map(ParameterSet::parameters)
                    .map(HashMap::new)
                    .orElseGet(() -> {
                        log.warnf("Parameter set %s not found, returning empty parameters", paramSetId);
                        return new HashMap<>();
                    });
        } catch (IllegalArgumentException e) {
            log.errorf("Invalid parameter set ID format: %s", paramSetIdStr);
            return new HashMap<>();
        }
    }

    /**
     * Checks if the given parameters use external storage.
     *
     * @param parameters the parameters map
     * @return true if external storage is used
     */
    public boolean usesExternalStorage(Map<String, Object> parameters) {
        return parameters != null && parameters.containsKey(PARAMETER_SET_ID_KEY);
    }

    /**
     * Extracts the parameter set ID if external storage is used.
     *
     * @param parameters the parameters map
     * @return the parameter set ID, or null if inline storage
     */
    public UUID getParameterSetId(Map<String, Object> parameters) {
        if (!usesExternalStorage(parameters)) {
            return null;
        }
        try {
            String paramSetIdStr = (String) parameters.get(PARAMETER_SET_ID_KEY);
            return UUID.fromString(paramSetIdStr);
        } catch (IllegalArgumentException e) {
            log.errorf("Invalid parameter set ID format in parameters");
            return null;
        }
    }
}

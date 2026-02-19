package ch.css.jobrunr.control.application.parameters;

import ch.css.jobrunr.control.domain.ParameterSet;
import ch.css.jobrunr.control.domain.ParameterStorageService;
import ch.css.jobrunr.control.domain.ScheduledJobInfo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Use Case: Resolves job parameters by loading external parameter sets when applicable.
 * <p>
 * When a job uses external parameter storage (@JobParameterSet), this use case loads
 * the actual parameters from the parameter storage using the job ID as the parameter set ID.
 */
@ApplicationScoped
public class ResolveParametersUseCase {

    private static final Logger LOG = Logger.getLogger(ResolveParametersUseCase.class);

    private final ParameterStorageService parameterStorageService;

    @Inject
    public ResolveParametersUseCase(ParameterStorageService parameterStorageService) {
        this.parameterStorageService = parameterStorageService;
    }

    /**
     * Resolves parameters for a job.
     * If the job uses external parameter storage, the parameters are loaded from the
     * parameter store using the job ID as the parameter set ID.
     * Otherwise, the inline parameters from the job info are returned.
     *
     * @param jobInfo the scheduled job info
     * @return the resolved parameters
     */
    public Map<String, Object> execute(ScheduledJobInfo jobInfo) {
        if (!jobInfo.hasExternalParameters()) {
            return new HashMap<>(jobInfo.getParameters());
        }

        UUID paramSetId = jobInfo.getJobId();
        return parameterStorageService.findById(paramSetId)
                .map(ParameterSet::parameters)
                .map(HashMap::new)
                .orElseGet(() -> {
                    LOG.warnf("Parameter set %s not found, returning inline parameters", paramSetId);
                    return new HashMap<>(jobInfo.getParameters());
                });
    }
}

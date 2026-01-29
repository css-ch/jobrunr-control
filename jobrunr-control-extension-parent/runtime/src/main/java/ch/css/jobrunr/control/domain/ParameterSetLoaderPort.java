package ch.css.jobrunr.control.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Port for loading parameter sets by job context.
 * Provides lazy loading capability for jobs.
 */
public interface ParameterSetLoaderPort {

    /**
     * Loads parameters for a job by job ID.
     * Handles both inline and external parameter resolution.
     *
     * @param jobId the job ID
     * @return the job parameters
     * @throws ParameterSetNotFoundException if external reference not found
     */
    Map<String, Object> loadParameters(UUID jobId);

    /**
     * Loads parameters directly by parameter set ID.
     * Only works for jobs using external parameter storage.
     *
     * @param parameterSetId the parameter set ID
     * @return the parameters
     * @throws ParameterSetNotFoundException if not found
     */
    Map<String, Object> loadParametersBySetId(UUID parameterSetId);
}

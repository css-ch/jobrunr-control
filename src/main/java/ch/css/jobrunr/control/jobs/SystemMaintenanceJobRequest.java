package ch.css.jobrunr.control.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;

public record SystemMaintenanceJobRequest(Boolean clearCache, Boolean compactDatabase,
                                          Boolean cleanupLogs) implements JobRequest {
    @Override
    public Class<SystemMaintenanceJob> getJobRequestHandler() {
        return SystemMaintenanceJob.class;
    }
}

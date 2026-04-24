package ch.css.jobrunr.control.jobs.archive;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDate;

public record LegacyCustomerDataArchivalAndCleanupJobRequest(
        @JobParameterDefinition(required = false, defaultValue = "2020-01-01")
        LocalDate archiveBefore,
        @JobParameterDefinition(required = false, defaultValue = "true")
        Boolean dryRun) implements JobRequest {

    @Override
    public Class<LegacyCustomerDataArchivalAndCleanupJobHandler> getJobRequestHandler() {
        return LegacyCustomerDataArchivalAndCleanupJobHandler.class;
    }
}

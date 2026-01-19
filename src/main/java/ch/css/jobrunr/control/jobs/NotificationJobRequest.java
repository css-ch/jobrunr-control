package ch.css.jobrunr.control.jobs;

import org.jobrunr.jobs.lambdas.JobRequest;

import java.time.LocalDateTime;

public record NotificationJobRequest(String recipientEmail, String subject, Boolean sendImmediately,
                                     LocalDateTime scheduledTime) implements JobRequest {
    @Override
    public Class<NotificationJob> getJobRequestHandler() {
        return NotificationJob.class;
    }
}

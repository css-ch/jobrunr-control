package ch.css.jobrunr.control.domain.details;

import java.util.List;

public record JobMessagesPaged(List<JobMessage> messages, long totalMessages, int page, int pageSize) {
}

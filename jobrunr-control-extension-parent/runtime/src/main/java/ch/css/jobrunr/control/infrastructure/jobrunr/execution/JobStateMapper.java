package ch.css.jobrunr.control.infrastructure.jobrunr.execution;

import ch.css.jobrunr.control.domain.JobAwaitingState;
import ch.css.jobrunr.control.domain.JobStatus;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.states.*;

/**
 * Maps JobRunr job states to domain JobStatus enums.
 */
@ApplicationScoped
public class JobStateMapper {

    private static final Logger LOG = Logger.getLogger(JobStateMapper.class);

    /**
     * Maps a JobRunr JobState to the domain JobStatus.
     *
     * @param jobState the JobRunr job state
     * @return the corresponding domain JobStatus
     */
    public JobStatus mapJobState(JobState jobState) {
        return switch (jobState) {
            case EnqueuedState s -> JobStatus.ENQUEUED;
            case ScheduledState s -> JobStatus.ENQUEUED;
            case AwaitingState s -> JobStatus.ENQUEUED;
            case ProcessingState s -> JobStatus.PROCESSING;
            case ProcessedState s -> JobStatus.PROCESSED;
            case SucceededState s -> JobStatus.SUCCEEDED;
            case FailedState s -> JobStatus.FAILED;
            case FailedBatchJobState s -> JobStatus.FAILED;
            case DeletedState s -> JobStatus.DELETED;
            case NotApplicableState s -> JobStatus.NOT_APPLICABLE;
            case null -> JobStatus.ENQUEUED;
            default -> {
                LOG.warnf("Unexpected job state: %s. Defaulting to ENQUEUED.", jobState.getName());
                yield JobStatus.ENQUEUED;
            }
        };
    }

    /**
     * Maps JobRunr's StateName to domain JobAwaitingState.
     *
     * @param stateName the JobRunr state name
     * @return the corresponding domain JobAwaitingState
     */
    public JobAwaitingState mapStateNameToJobAwaitingState(StateName stateName) {
        return switch (stateName) {
            case SUCCEEDED -> JobAwaitingState.SUCCEEDED;
            case FAILED -> JobAwaitingState.FAILED;
            default -> JobAwaitingState.SUCCEEDED; // fallback
        };
    }
}

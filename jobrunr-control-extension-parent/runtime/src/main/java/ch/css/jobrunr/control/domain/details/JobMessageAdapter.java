package ch.css.jobrunr.control.domain.details;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class JobMessageAdapter implements JobMessageService {

    private final JobMessageStoragePort jobMessageStorage;

    @Inject
    public JobMessageAdapter(JobMessageStoragePort jobMessageStorage) {
        this.jobMessageStorage = jobMessageStorage;
    }


    @Override
    public void info(String message, Object... args) {
        writeMessage(JobMessageLevel.INFO, String.format(message, args), null);
    }

    @Override
    public void warning(String message, Object... args) {
        writeMessage(JobMessageLevel.WARNING, String.format(message, args), null);
    }

    @Override
    public void error(String message, Object... args) {
        writeMessage(JobMessageLevel.ERROR, String.format(message, args), null);
    }

    @Override
    public void exception(Throwable throwable) {
        writeMessage(JobMessageLevel.EXCEPTION, "An exception occurred: " + throwable.getMessage(), stackTraceAsString(throwable));
    }

    private static String stackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private void writeMessage(JobMessageLevel level, String message, String stackTrace) {
        UUID batchJobId = ThreadLocalJobContext.getJobContext().getAwaitedJobId();
        UUID jobId = null;
        if(batchJobId == null) {
            batchJobId = ThreadLocalJobContext.getJobContext().getJobId();
        } else {
            jobId = ThreadLocalJobContext.getJobContext().getJobId();
        }
        JobMessage jobMessage = new JobMessage(Instant.now(), jobId, level, message, stackTrace);
        jobMessageStorage.writeMessage(batchJobId, jobMessage);
    }
}

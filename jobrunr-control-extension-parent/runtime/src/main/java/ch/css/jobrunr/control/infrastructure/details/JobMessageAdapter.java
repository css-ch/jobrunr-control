package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.domain.details.JobMessage;
import ch.css.jobrunr.control.domain.details.JobMessageLevel;
import ch.css.jobrunr.control.domain.details.JobMessageService;
import ch.css.jobrunr.control.domain.details.JobMessageStoragePort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
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
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void infoTxNew(String message, Object... args) {
        writeMessage(JobMessageLevel.INFO, String.format(message, args), null);
    }

    @Override
    public void warning(String message, Object... args) {
        writeMessage(JobMessageLevel.WARNING, String.format(message, args), null);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void warningTxNew(String message, Object... args) {
        writeMessage(JobMessageLevel.WARNING, String.format(message, args), null);
    }

    @Override
    public void error(String message, Object... args) {
        writeMessage(JobMessageLevel.ERROR, String.format(message, args), null);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void errorTxNew(String message, Object... args) {
        writeMessage(JobMessageLevel.ERROR, String.format(message, args), null);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void exception(String message, Throwable throwable) {
        writeMessage(JobMessageLevel.EXCEPTION, message, stackTraceAsString(throwable));
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void exception(String message, Object args1, Throwable throwable) {
        writeMessage(JobMessageLevel.EXCEPTION, String.format(message, args1), stackTraceAsString(throwable));
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void exception(String message, Object args1, Object args2, Throwable throwable) {
        writeMessage(JobMessageLevel.EXCEPTION, String.format(message, args1, args2), stackTraceAsString(throwable));
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void exception(String message, Object args1, Object args2, Object args3, Throwable throwable) {
        writeMessage(JobMessageLevel.EXCEPTION, String.format(message, args1, args2, args3), stackTraceAsString(throwable));
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void exception(String message, Object args1, Object args2, Object args3, Object args4, Throwable throwable) {
        writeMessage(JobMessageLevel.EXCEPTION, String.format(message, args1, args2, args3, args4), stackTraceAsString(throwable));
    }

    private static String stackTraceAsString(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }

    private void writeMessage(JobMessageLevel level, String message, String stackTrace) {
        UUID batchJobId = ThreadLocalJobContext.getJobContext().getAwaitedJobId();
        UUID jobId = ThreadLocalJobContext.getJobContext().getJobId();
        if(batchJobId == null) {
            batchJobId = jobId;
        }
        JobMessage jobMessage = new JobMessage(Instant.now(), jobId, level, message, stackTrace);
        jobMessageStorage.writeMessage(batchJobId, jobMessage);
    }
}

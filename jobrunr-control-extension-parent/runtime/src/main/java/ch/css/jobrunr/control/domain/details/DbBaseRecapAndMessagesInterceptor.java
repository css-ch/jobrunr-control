package ch.css.jobrunr.control.domain.details;

import ch.css.jobrunr.control.annotations.DbBasedRecapAndMessages;
import ch.css.jobrunr.control.infrastructure.details.RecapValueExtractorRegistry;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jobrunr.server.runner.ThreadLocalJobContext;

import java.util.Map;
import java.util.UUID;

/**
 * Stores recap values and exception details for JobRunr result handlers using database-backed ports.
 */
@DbBasedRecapAndMessages
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class DbBaseRecapAndMessagesInterceptor {

    private final JobMessageService jobMessageService;
    private final JobRecapStoragePort jobRecapStoragePort;
    private final RecapValueExtractorRegistry recapValueExtractorRegistry;

    @Inject
    public DbBaseRecapAndMessagesInterceptor(JobMessageService jobMessageService,
                                             JobRecapStoragePort jobRecapStoragePort,
                                             RecapValueExtractorRegistry recapValueExtractorRegistry) {
        this.jobMessageService = jobMessageService;
        this.jobRecapStoragePort = jobRecapStoragePort;
        this.recapValueExtractorRegistry = recapValueExtractorRegistry;
    }

    @AroundInvoke
    public Object persistRecapAndExceptions(InvocationContext invocationContext) throws Exception {
        String methodName = invocationContext.getMethod().getName();
        boolean isRunAndReturn = "runAndReturn".equals(methodName);
        boolean isRun = "run".equals(methodName);
        if (!isRunAndReturn && !isRun) {
            return invocationContext.proceed();
        }
        try {
            Object resultObject = invocationContext.proceed();
            if (isRunAndReturn) {
                writeChildRecap(resultObject);
            }
            return resultObject;
        } catch (Exception exception) {
            jobMessageService.exception("Exception while processing Worker", exception);
            throw exception;
        }
    }

    private void writeChildRecap(Object resultObject) {
        if (resultObject == null) {
            return;
        }
        recapValueExtractorRegistry.findByRecapClassName(resultObject.getClass().getName())
                .ifPresent(extractor -> {
                    try {
                        Map<String, Long> recapValues = extractor.extract(resultObject);
                        if (recapValues != null && !recapValues.isEmpty()) {
                            UUID jobId = ThreadLocalJobContext.getJobContext().getJobId();
                            UUID batchJobId = ThreadLocalJobContext.getJobContext().getAwaitedJobId();
                            jobRecapStoragePort.writeRecap(batchJobId, jobId, recapValues);
                        }
                    } catch (Exception exception) {
                        jobMessageService.exception("Exception while processing Recap-Values", exception);
                    }
                });
    }
}

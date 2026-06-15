package ch.css.jobrunr.control.infrastructure.details;

import ch.css.jobrunr.control.annotations.DbBasedRecapAndMessages;
import ch.css.jobrunr.control.domain.details.JobMessageService;
import ch.css.jobrunr.control.domain.details.JobRecapService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import java.util.Map;

/**
 * Stores recap values and exception details for JobRunr result handlers using database-backed ports.
 */
@DbBasedRecapAndMessages
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 10) // Priority after @Transactional
public class DbBasedRecapAndMessagesInterceptor {

    private final JobMessageService messageService;
    private final JobRecapService recapService;
    private final RecapValueExtractorRegistry recapValueExtractorRegistry;

    @Inject
    public DbBasedRecapAndMessagesInterceptor(JobMessageService messageService,
                                              JobRecapService recapService,
                                              RecapValueExtractorRegistry recapValueExtractorRegistry) {
        this.messageService = messageService;
        this.recapService = recapService;
        this.recapValueExtractorRegistry = recapValueExtractorRegistry;
    }

    @AroundInvoke
    public Object persistRecapAndExceptions(InvocationContext invocationContext) throws Exception {
        try {
            Object resultObject = invocationContext.proceed();
            writeChildRecap(resultObject);
            return resultObject;
        } catch (Exception exception) {
            messageService.exception("Exception while processing Worker", exception);
            throw exception;
        }
    }

    private void writeChildRecap(Object resultObject) {
        if (resultObject == null) {
            return;
        }
        recapValueExtractorRegistry.findByRecapClassName(resultObject.getClass().getName())
                .ifPresent(extractor -> {
                    Map<String, Long> recapValues = extractor.extract(resultObject);
                    if (recapValues != null && !recapValues.isEmpty()) {
                        recapService.writeRecap(recapValues);
                    }
                });
    }
}

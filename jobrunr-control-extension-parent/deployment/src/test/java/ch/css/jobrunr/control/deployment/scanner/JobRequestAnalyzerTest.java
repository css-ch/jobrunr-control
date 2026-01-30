package ch.css.jobrunr.control.deployment.scanner;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JobRequestAnalyzer.
 * Verifies type resolution, record detection, and interface hierarchy traversal.
 */
class JobRequestAnalyzerTest {

    private IndexView index;
    private JobRequestAnalyzer analyzer;

    @BeforeEach
    void setup() throws IOException {
        Indexer indexer = new Indexer();
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, ValidRecordRequest.class);
        indexClass(indexer, DirectHandlerImplementation.class);
        indexClass(indexer, NotARecordRequest.class);
        indexClass(indexer, NotARecordHandler.class);
        index = indexer.complete();
        analyzer = new JobRequestAnalyzer(index);
    }

    @Test
    void shouldFindJobRequestTypeFromDirectImplementation() {
        ClassInfo handlerClass = index.getClassByName(DirectHandlerImplementation.class.getName());

        Type jobRequestType = analyzer.findJobRequestType(handlerClass);

        assertNotNull(jobRequestType, "Should find JobRequest type from direct implementation");
        assertEquals(ValidRecordRequest.class.getName(), jobRequestType.name().toString());
    }

    @Test
    void shouldReturnNullWhenJobRequestIsNotARecord() {
        ClassInfo handlerClass = index.getClassByName(NotARecordHandler.class.getName());

        Type jobRequestType = analyzer.findJobRequestType(handlerClass);

        assertNull(jobRequestType, "Should return null when JobRequest type is not a record");
    }

    // Test classes

    public record ValidRecordRequest(String data) implements JobRequest {
        @Override
        public Class<DirectHandlerImplementation> getJobRequestHandler() {
            return DirectHandlerImplementation.class;
        }
    }

    public static class DirectHandlerImplementation implements JobRequestHandler<ValidRecordRequest> {
        @Override
        public void run(ValidRecordRequest jobRequest) {
        }
    }

    public static class NotARecordRequest implements JobRequest {
        @Override
        public Class<NotARecordHandler> getJobRequestHandler() {
            return NotARecordHandler.class;
        }
    }

    public static class NotARecordHandler implements JobRequestHandler<NotARecordRequest> {
        @Override
        public void run(NotARecordRequest jobRequest) {
        }
    }

    private void indexClass(Indexer indexer, Class<?> clazz) throws IOException {
        String className = clazz.getName().replace('.', '/') + ".class";
        try (InputStream stream = clazz.getClassLoader().getResourceAsStream(className)) {
            if (stream != null) {
                indexer.index(stream);
            } else {
                throw new IOException("Could not find class file: " + className);
            }
        }
    }
}

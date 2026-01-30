package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.domain.JobSettings;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.MethodInfo;
import org.jobrunr.jobs.filters.JobFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JobSettingsExtractor.
 * Verifies correct extraction of job configuration from @ConfigurableJob annotations.
 */
class JobSettingsExtractorTest {

    private IndexView index;
    private JobSettingsExtractor extractor;

    @BeforeEach
    void setup() throws IOException {
        Indexer indexer = new Indexer();
        indexClass(indexer, ConfigurableJob.class);
        indexClass(indexer, MinimalConfiguredJob.class);
        indexClass(indexer, FullyConfiguredJob.class);
        indexClass(indexer, BatchJob.class);
        indexClass(indexer, JobWithRetries.class);
        indexClass(indexer, JobWithMultipleLabels.class);
        indexClass(indexer, JobWithFilters.class);
        indexClass(indexer, DummyJobFilter.class);
        indexClass(indexer, NoConfigurableJobAnnotation.class);
        index = indexer.complete();
        extractor = new JobSettingsExtractor();
    }

    @Test
    void shouldExtractDefaultSettings() {
        ClassInfo classInfo = index.getClassByName(MinimalConfiguredJob.class.getName());
        MethodInfo method = extractor.findConfigurableJobMethod(classInfo);

        assertNotNull(method, "Should find @ConfigurableJob annotated method");

        JobSettings settings = extractor.extractJobSettings(method);

        assertEquals("", settings.name());
        assertFalse(settings.isBatch());
        assertEquals(ConfigurableJob.NBR_OF_RETRIES_NOT_PROVIDED, settings.retries());
        assertTrue(settings.labels().isEmpty());
        assertTrue(settings.jobFilters().isEmpty());
        assertEquals("", settings.queue());
        assertEquals("", settings.runOnServerWithTag());
        assertEquals("", settings.mutex());
        assertEquals("", settings.rateLimiter());
        assertEquals("", settings.processTimeOut());
        assertEquals("", settings.deleteOnSuccess());
        assertEquals("", settings.deleteOnFailure());
    }

    @Test
    void shouldExtractFullConfiguration() {
        ClassInfo classInfo = index.getClassByName(FullyConfiguredJob.class.getName());
        MethodInfo method = extractor.findConfigurableJobMethod(classInfo);

        JobSettings settings = extractor.extractJobSettings(method);

        assertEquals("Full Job", settings.name());
        assertTrue(settings.isBatch());
        assertEquals(5, settings.retries());
        assertEquals(2, settings.labels().size());
        assertTrue(settings.labels().contains("important"));
        assertTrue(settings.labels().contains("critical"));
        assertEquals("high-priority", settings.queue());
        assertEquals("production", settings.runOnServerWithTag());
        assertEquals("job-mutex", settings.mutex());
        assertEquals("rate-limiter-1", settings.rateLimiter());
        assertEquals("PT2H", settings.processTimeOut());
        assertEquals("PT24H", settings.deleteOnSuccess());
        assertEquals("PT72H", settings.deleteOnFailure());
    }

    @Test
    void shouldDetectBatchJobFlag() {
        ClassInfo batchClass = index.getClassByName(BatchJob.class.getName());
        MethodInfo batchMethod = extractor.findConfigurableJobMethod(batchClass);

        assertTrue(extractor.getBatchJobFlag(batchMethod));

        ClassInfo nonBatchClass = index.getClassByName(MinimalConfiguredJob.class.getName());
        MethodInfo nonBatchMethod = extractor.findConfigurableJobMethod(nonBatchClass);

        assertFalse(extractor.getBatchJobFlag(nonBatchMethod));
    }

    @Test
    void shouldExtractCustomRetries() {
        ClassInfo classInfo = index.getClassByName(JobWithRetries.class.getName());
        MethodInfo method = extractor.findConfigurableJobMethod(classInfo);

        JobSettings settings = extractor.extractJobSettings(method);

        assertEquals(10, settings.retries());
    }

    @Test
    void shouldExtractMultipleLabels() {
        ClassInfo classInfo = index.getClassByName(JobWithMultipleLabels.class.getName());
        MethodInfo method = extractor.findConfigurableJobMethod(classInfo);

        JobSettings settings = extractor.extractJobSettings(method);

        assertEquals(3, settings.labels().size());
        assertTrue(settings.labels().contains("label1"));
        assertTrue(settings.labels().contains("label2"));
        assertTrue(settings.labels().contains("label3"));
    }

    @Test
    void shouldExtractJobFilters() {
        ClassInfo classInfo = index.getClassByName(JobWithFilters.class.getName());
        MethodInfo method = extractor.findConfigurableJobMethod(classInfo);

        JobSettings settings = extractor.extractJobSettings(method);

        assertEquals(1, settings.jobFilters().size());
        assertTrue(settings.jobFilters().contains(DummyJobFilter.class.getName()));
    }

    @Test
    void shouldReturnNullWhenNoConfigurableJobMethod() {
        ClassInfo classInfo = index.getClassByName(NoConfigurableJobAnnotation.class.getName());

        MethodInfo method = extractor.findConfigurableJobMethod(classInfo);

        assertNull(method, "Should return null when no @ConfigurableJob method exists");
    }

    // Test classes

    public static class MinimalConfiguredJob {
        @ConfigurableJob
        public void run() {
        }
    }

    public static class FullyConfiguredJob {
        @ConfigurableJob(
                name = "Full Job",
                isBatch = true,
                retries = 5,
                labels = {"important", "critical"},
                jobFilters = {},
                queue = "high-priority",
                runOnServerWithTag = "production",
                mutex = "job-mutex",
                rateLimiter = "rate-limiter-1",
                processTimeOut = "PT2H",
                deleteOnSuccess = "PT24H",
                deleteOnFailure = "PT72H"
        )
        public void run() {
        }
    }

    public static class BatchJob {
        @ConfigurableJob(isBatch = true)
        public void run() {
        }
    }

    public static class JobWithRetries {
        @ConfigurableJob(retries = 10)
        public void run() {
        }
    }

    public static class JobWithMultipleLabels {
        @ConfigurableJob(labels = {"label1", "label2", "label3"})
        public void run() {
        }
    }

    public static class JobWithFilters {
        @ConfigurableJob(jobFilters = {DummyJobFilter.class})
        public void run() {
        }
    }

    public static class DummyJobFilter implements JobFilter {
    }

    public static class NoConfigurableJobAnnotation {
        public void run() {
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

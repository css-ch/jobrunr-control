package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the JobDefinitionIndexScanner class.
 * Verifies that job specifications are correctly extracted from annotated classes.
 */
class JobDefinitionIndexScannerTest {

    @Test
    void testFindJobSpecifications_withSimpleJob() throws IOException {
        // Create a separate index with only the simple job
        Indexer indexer = new Indexer();
        indexClass(indexer, SimpleJobHandler.class);
        indexClass(indexer, SimpleJobRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        IndexView simpleIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(simpleIndex);

        assertNotNull(results, "JobDescriptions should be found");
        assertEquals(1, results.size(), "Should have exactly 1 job description");

        JobDefinition result = results.iterator().next();
        assertEquals("SimpleJobHandler", result.jobType());
        assertFalse(result.isBatchJob(), "Should benot a batch job by default");
        assertEquals(SimpleJobRequest.class.getName(), result.jobRequestTypeName());

        // Verify parameters
        assertNotNull(result.parameters());
        assertEquals(1, result.parameters().size(), "Should have 1 parameter");

        ch.css.jobrunr.control.domain.JobParameter param = result.parameters().getFirst();
        assertEquals("message", param.name());
        assertEquals(JobParameterType.STRING, param.type());
        assertTrue(param.required(), "Parameter without default value should be required");
        assertNull(param.defaultValue());
    }

    @Test
    void testFindJobSpecifications_withBatchJobFalse() throws IOException {
        // Create a separate index with only the batch job
        Indexer indexer = new Indexer();
        indexClass(indexer, BatchJobHandler.class);
        indexClass(indexer, BatchJobRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        IndexView batchIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(batchIndex);

        assertNotNull(results, "JobDescriptions should be found");
        assertEquals(1, results.size(), "Should have exactly 1 job description");

        JobDefinition result = results.iterator().next();
        assertEquals("BatchJobHandler", result.jobType());
        assertFalse(result.isBatchJob(), "Should not be a batch job");
        assertEquals(BatchJobRequest.class.getName(), result.jobRequestTypeName());
    }

    @Test
    void testFindJobSpecifications_withComplexParameters() throws IOException {
        // Create a separate index with only the complex job
        Indexer indexer = new Indexer();
        indexClass(indexer, ComplexJobHandler.class);
        indexClass(indexer, ComplexJobRequest.class);
        indexClass(indexer, EnumParameter.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        IndexView complexIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(complexIndex);

        assertNotNull(results, "JobDescriptions should be found");
        assertEquals(1, results.size(), "Should have exactly 1 job description");

        JobDefinition result = results.iterator().next();
        assertEquals("ComplexJobHandler", result.jobType());
        assertFalse(result.isBatchJob());

        // Verify all parameters
        assertEquals(6, result.parameters().size(), "Should have 6 parameters");

        // Check string parameter with default value
        ch.css.jobrunr.control.domain.JobParameter stringParam = findParameter(result, "stringParam");
        assertNotNull(stringParam);
        assertEquals(JobParameterType.STRING, stringParam.type());
        assertFalse(stringParam.required(), "Parameter with default value should not be required");
        assertEquals("default", stringParam.defaultValue());

        // Check integer parameter without default
        ch.css.jobrunr.control.domain.JobParameter intParam = findParameter(result, "intParam");
        assertNotNull(intParam);
        assertEquals(JobParameterType.INTEGER, intParam.type());
        assertTrue(intParam.required());
        assertNull(intParam.defaultValue());

        // Check boolean parameter with default
        ch.css.jobrunr.control.domain.JobParameter boolParam = findParameter(result, "boolParam");
        assertNotNull(boolParam);
        assertEquals(JobParameterType.BOOLEAN, boolParam.type());
        assertFalse(boolParam.required());
        assertEquals("true", boolParam.defaultValue());

        // Check date parameter
        ch.css.jobrunr.control.domain.JobParameter dateParam = findParameter(result, "dateParam");
        assertNotNull(dateParam);
        assertEquals(JobParameterType.DATE, dateParam.type());
        assertFalse(dateParam.required());
        assertEquals("2024-01-01", dateParam.defaultValue());

        // Check datetime parameter
        ch.css.jobrunr.control.domain.JobParameter dateTimeParam = findParameter(result, "dateTimeParam");
        assertNotNull(dateTimeParam);
        assertEquals(JobParameterType.DATETIME, dateTimeParam.type());
        assertTrue(dateTimeParam.required());

        // Check enum parameter
        ch.css.jobrunr.control.domain.JobParameter enumParam = findParameter(result, "enumParam");
        assertNotNull(enumParam);
        assertEquals(JobParameterType.ENUM, enumParam.type());
        assertFalse(enumParam.required());
        assertEquals("OPTION_A", enumParam.defaultValue());
    }

    @Test
    void testFindJobSpecifications_withCustomParameterName() throws IOException {
        // Test all parameter types with custom names and various default value configurations
        Indexer indexer = new Indexer();
        indexClass(indexer, CustomParameterJobHandler.class);
        indexClass(indexer, CustomParameterJobRequest.class);
        indexClass(indexer, EnumParameter.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        indexClass(indexer, JobParameterDefinition.class);
        IndexView customIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(customIndex);

        assertNotNull(results, "JobDescriptions should be found");
        assertEquals(1, results.size(), "Should have exactly 1 job description");

        JobDefinition result = results.iterator().next();
        assertEquals("CustomParameterJobHandler", result.jobType());
        assertEquals(6, result.parameters().size(), "Should have 6 parameters with different configurations");

        // Check string parameter with custom name and default value
        JobParameter customString = findParameter(result, "customStringName");
        assertNotNull(customString, "Custom string parameter should exist");
        assertEquals(JobParameterType.STRING, customString.type());
        assertFalse(customString.required(), "Parameter with default value should not be required");
        assertEquals("customDefault", customString.defaultValue());

        // Check integer parameter with custom name but no default
        JobParameter customInt = findParameter(result, "customIntName");
        assertNotNull(customInt, "Custom integer parameter should exist");
        assertEquals(JobParameterType.INTEGER, customInt.type());
        assertTrue(customInt.required(), "Parameter without default value should be required");
        assertNull(customInt.defaultValue());

        // Check boolean parameter with custom name and default
        JobParameter customBool = findParameter(result, "customBoolName");
        assertNotNull(customBool, "Custom boolean parameter should exist");
        assertEquals(JobParameterType.BOOLEAN, customBool.type());
        assertFalse(customBool.required());
        assertEquals("false", customBool.defaultValue());

        // Check date parameter with custom name and default
        JobParameter customDate = findParameter(result, "customDateName");
        assertNotNull(customDate, "Custom date parameter should exist");
        assertEquals(JobParameterType.DATE, customDate.type());
        assertFalse(customDate.required());
        assertEquals("2025-12-31", customDate.defaultValue());

        // Check datetime parameter with custom name but no default
        JobParameter customDateTime = findParameter(result, "customDateTimeName");
        assertNotNull(customDateTime, "Custom datetime parameter should exist");
        assertEquals(JobParameterType.DATETIME, customDateTime.type());
        assertTrue(customDateTime.required());
        assertNull(customDateTime.defaultValue());

        // Check enum parameter with custom name and default
        JobParameter customEnum = findParameter(result, "customEnumName");
        assertNotNull(customEnum, "Custom enum parameter should exist");
        assertEquals(JobParameterType.ENUM, customEnum.type());
        assertFalse(customEnum.required());
        assertEquals("OPTION_B", customEnum.defaultValue());
    }

    @Test
    void testFindJobSpecifications_noMatchingJob() throws IOException {
        // Create an index without any @ConfigurableJob annotated classes
        Indexer indexer = new Indexer();
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        IndexView emptyIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(emptyIndex);

        assertNotNull(results, "Should return a set even when empty");
        assertTrue(results.isEmpty(), "Should return empty set when no matching job is found");
    }

    private JobParameter findParameter(JobDefinition description, String name) {
        return description.parameters().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElse(null);
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

    // Test classes

    /**
     * Simple job request with one string parameter
     */
    public record SimpleJobRequest(String message) implements JobRequest {
        @Override
        public Class<SimpleJobHandler> getJobRequestHandler() {
            return SimpleJobHandler.class;
        }
    }

    /**
     * Simple job handler with @ConfigurableJob annotation (default isBatch=true)
     */
    public static class SimpleJobHandler implements JobRequestHandler<SimpleJobRequest> {
        @ConfigurableJob
        @Override
        public void run(SimpleJobRequest request) {
            // Test implementation
        }
    }

    /**
     * Batch job request
     */
    public record BatchJobRequest(String data) implements JobRequest {
        @Override
        public Class<BatchJobHandler> getJobRequestHandler() {
            return BatchJobHandler.class;
        }
    }

    /**
     * Batch job handler with isBatch=false
     */
    public static class BatchJobHandler implements JobRequestHandler<BatchJobRequest> {
        @ConfigurableJob(isBatch = false)
        @Override
        public void run(BatchJobRequest request) {
            // Test implementation
        }
    }

    /**
     * Complex job request with multiple parameter types
     */
    public record ComplexJobRequest(
            @JobParameterDefinition(defaultValue = "default") String stringParam,
            Integer intParam,
            @JobParameterDefinition(defaultValue = "true") Boolean boolParam,
            @JobParameterDefinition(defaultValue = "2024-01-01") LocalDate dateParam,
            LocalDateTime dateTimeParam,
            @JobParameterDefinition(defaultValue = "OPTION_A") EnumParameter enumParam
    ) implements JobRequest {
        @Override
        public Class<ComplexJobHandler> getJobRequestHandler() {
            return ComplexJobHandler.class;
        }
    }

    /**
     * Complex job handler
     */
    public static class ComplexJobHandler implements JobRequestHandler<ComplexJobRequest> {
        @ConfigurableJob
        @Override
        public void run(ComplexJobRequest request) {
            // Test implementation
        }
    }

    /**
     * Test enum for parameter testing
     */
    public enum EnumParameter {
        OPTION_A, OPTION_B, OPTION_C
    }

    /**
     * Custom parameter job request with all parameter types using custom names
     */
    public record CustomParameterJobRequest(
            @JobParameterDefinition(name = "customStringName", defaultValue = "customDefault") String stringField,
            @JobParameterDefinition(name = "customIntName") Integer intField,
            @JobParameterDefinition(name = "customBoolName", defaultValue = "false") Boolean boolField,
            @JobParameterDefinition(name = "customDateName", defaultValue = "2025-12-31") LocalDate dateField,
            @JobParameterDefinition(name = "customDateTimeName") LocalDateTime dateTimeField,
            @JobParameterDefinition(name = "customEnumName", defaultValue = "OPTION_B") EnumParameter enumField
    ) implements JobRequest {
        @Override
        public Class<CustomParameterJobHandler> getJobRequestHandler() {
            return CustomParameterJobHandler.class;
        }
    }

    /**
     * Custom parameter job handler
     */
    public static class CustomParameterJobHandler implements JobRequestHandler<CustomParameterJobRequest> {
        @ConfigurableJob
        @Override
        public void run(CustomParameterJobRequest request) {
            // Test implementation
        }
    }
}

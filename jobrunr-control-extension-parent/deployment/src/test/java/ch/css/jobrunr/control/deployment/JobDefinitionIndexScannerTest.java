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
    void testFindJobSpecifications_jobTypeStaysHandlerSimpleNameEvenWithAnnotationName() throws IOException {
        // Regression for the bug where jobType fell back to @ConfigurableJob.name(), breaking
        // JobRunrSchedulerAdapter.mapToScheduledJobInfo (which looks up definitions via the
        // handler simple class name derived from JobDetails.getClassName()). If this test fails,
        // scheduled jobs and templates with a named @ConfigurableJob will disappear from the UI.
        Indexer indexer = new Indexer();
        indexClass(indexer, NamedJobHandler.class);
        indexClass(indexer, NamedJobRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        IndexView namedIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(namedIndex);

        assertEquals(1, results.size());
        JobDefinition result = results.iterator().next();
        assertEquals("NamedJobHandler", result.jobType(),
                "jobType must stay the handler simple name so the runtime lookup by JobDetails.getClassName() keeps working");
        assertEquals("My Named Job", result.jobSettings().name(),
                "The display name from @ConfigurableJob must remain accessible via JobSettings.name()");
    }

    @Test
    void testFindJobSpecifications_failsOnDuplicateHandlerSimpleName() throws IOException {
        // Two handlers living in different nested classes but sharing the same simple name
        // would otherwise silently collide in findJobByType() and in the "jobtype:" label.
        Indexer indexer = new Indexer();
        indexClass(indexer, DuplicateSimpleNameContainerA.CollidingHandler.class);
        indexClass(indexer, DuplicateSimpleNameContainerA.CollidingRequest.class);
        indexClass(indexer, DuplicateSimpleNameContainerB.CollidingHandler.class);
        indexClass(indexer, DuplicateSimpleNameContainerB.CollidingRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        IndexView duplicateIndex = indexer.complete();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JobDefinitionIndexScanner.findJobSpecifications(duplicateIndex));

        assertTrue(ex.getMessage().contains("CollidingHandler"),
                "Error message should name the colliding jobType; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(DuplicateSimpleNameContainerA.CollidingHandler.class.getName()),
                "Error message should list the first colliding FQN; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(DuplicateSimpleNameContainerB.CollidingHandler.class.getName()),
                "Error message should list the second colliding FQN; was: " + ex.getMessage());
    }

    @Test
    void testFindJobSpecifications_customJobTypeFromAnnotation() throws IOException {
        // @ConfigurableJob(jobType = "...") overrides the default simple-class-name based jobType
        // so handlers with long class names can still produce a label that fits JobRunr's limit.
        Indexer indexer = new Indexer();
        indexClass(indexer, CustomJobTypeHandlerWithLongClassName.class);
        indexClass(indexer, CustomJobTypeRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        IndexView customTypeIndex = indexer.complete();

        var results = JobDefinitionIndexScanner.findJobSpecifications(customTypeIndex);

        assertEquals(1, results.size());
        JobDefinition result = results.iterator().next();
        assertEquals("ShortType", result.jobType(),
                "jobType must come from @ConfigurableJob(jobType) when the attribute is set");
        assertEquals("My Display Name", result.jobSettings().name(),
                "Display name must remain independent of the jobType override");
    }

    @Test
    void testFindJobSpecifications_failsOnJobTypeTooLong() throws IOException {
        // Simulates a handler whose simple class name alone would blow the 45-char label limit
        // once the "jobtype:" prefix is prepended. The scanner must fail the build and point
        // developers at the escape hatch.
        Indexer indexer = new Indexer();
        indexClass(indexer, HandlerWithClassNameExceedingJobTypeBudgetAndThenSome.class);
        indexClass(indexer, TooLongRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        IndexView tooLongIndex = indexer.complete();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JobDefinitionIndexScanner.findJobSpecifications(tooLongIndex));

        assertTrue(ex.getMessage().contains("jobType"),
                "Error message should mention jobType; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("@ConfigurableJob(jobType"),
                "Error message should point at the escape hatch; was: " + ex.getMessage());
    }

    @Test
    void testFindJobSpecifications_failsOnInvalidJobTypeCharacters() throws IOException {
        // A jobType override must only contain [A-Za-z0-9_-] so it is safe for URLs, search
        // filters, and the "jobtype:" label.
        Indexer indexer = new Indexer();
        indexClass(indexer, InvalidCharJobTypeHandler.class);
        indexClass(indexer, InvalidCharRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        IndexView invalidCharIndex = indexer.complete();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JobDefinitionIndexScanner.findJobSpecifications(invalidCharIndex));

        assertTrue(ex.getMessage().contains("[A-Za-z0-9_-]"),
                "Error message should mention the allowed character set; was: " + ex.getMessage());
    }

    @Test
    void testFindJobSpecifications_failsOnDuplicateEffectiveJobType() throws IOException {
        // Two handlers with distinct simple names but identical jobType overrides must still
        // be rejected, because the effective jobType is the lookup key.
        Indexer indexer = new Indexer();
        indexClass(indexer, FirstHandlerWithSharedOverride.class);
        indexClass(indexer, FirstSharedRequest.class);
        indexClass(indexer, SecondHandlerWithSharedOverride.class);
        indexClass(indexer, SecondSharedRequest.class);
        indexClass(indexer, JobRequestHandler.class);
        indexClass(indexer, JobRequest.class);
        indexClass(indexer, ConfigurableJob.class);
        IndexView sharedOverrideIndex = indexer.complete();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> JobDefinitionIndexScanner.findJobSpecifications(sharedOverrideIndex));

        assertTrue(ex.getMessage().contains("Shared"),
                "Error message should include the conflicting jobType; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(FirstHandlerWithSharedOverride.class.getName()),
                "Error message should list the first conflicting handler; was: " + ex.getMessage());
        assertTrue(ex.getMessage().contains(SecondHandlerWithSharedOverride.class.getName()),
                "Error message should list the second conflicting handler; was: " + ex.getMessage());
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
            @JobParameterDefinition(required = false, defaultValue = "default") String stringParam,
            Integer intParam,
            @JobParameterDefinition(required = false, defaultValue = "true") Boolean boolParam,
            @JobParameterDefinition(required = false, defaultValue = "2024-01-01") LocalDate dateParam,
            LocalDateTime dateTimeParam,
            @JobParameterDefinition(required = false, defaultValue = "OPTION_A") EnumParameter enumParam
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
            @JobParameterDefinition(name = "customStringName", required = false, defaultValue = "customDefault") String stringField,
            @JobParameterDefinition(name = "customIntName") Integer intField,
            @JobParameterDefinition(name = "customBoolName", required = false, defaultValue = "false") Boolean boolField,
            @JobParameterDefinition(name = "customDateName", required = false, defaultValue = "2025-12-31") LocalDate dateField,
            @JobParameterDefinition(name = "customDateTimeName") LocalDateTime dateTimeField,
            @JobParameterDefinition(name = "customEnumName", required = false, defaultValue = "OPTION_B") EnumParameter enumField
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

    /**
     * Job request for a handler that carries a @ConfigurableJob(name = ...) annotation.
     */
    public record NamedJobRequest() implements JobRequest {
        @Override
        public Class<NamedJobHandler> getJobRequestHandler() {
            return NamedJobHandler.class;
        }
    }

    /**
     * Handler with an explicit display name — used to prove that the display name does not
     * leak into JobDefinition.jobType.
     */
    public static class NamedJobHandler implements JobRequestHandler<NamedJobRequest> {
        @ConfigurableJob(name = "My Named Job")
        @Override
        public void run(NamedJobRequest request) {
            // Test implementation
        }
    }

    /**
     * First container carrying a handler whose simple name collides with another handler in
     * {@link DuplicateSimpleNameContainerB}. Used to verify the uniqueness guard.
     */
    public static final class DuplicateSimpleNameContainerA {
        public record CollidingRequest() implements JobRequest {
            @Override
            public Class<CollidingHandler> getJobRequestHandler() {
                return CollidingHandler.class;
            }
        }

        public static class CollidingHandler implements JobRequestHandler<CollidingRequest> {
            @ConfigurableJob
            @Override
            public void run(CollidingRequest request) {
                // Test implementation
            }
        }
    }

    /**
     * Second container with a handler that intentionally shares a simple name with
     * {@link DuplicateSimpleNameContainerA}.
     */
    public static final class DuplicateSimpleNameContainerB {
        public record CollidingRequest() implements JobRequest {
            @Override
            public Class<CollidingHandler> getJobRequestHandler() {
                return CollidingHandler.class;
            }
        }

        public static class CollidingHandler implements JobRequestHandler<CollidingRequest> {
            @ConfigurableJob
            @Override
            public void run(CollidingRequest request) {
                // Test implementation
            }
        }
    }

    /**
     * Job request for {@link CustomJobTypeHandlerWithLongClassName}.
     */
    public record CustomJobTypeRequest() implements JobRequest {
        @Override
        public Class<CustomJobTypeHandlerWithLongClassName> getJobRequestHandler() {
            return CustomJobTypeHandlerWithLongClassName.class;
        }
    }

    /**
     * Handler with a long class name that opts into a short, explicit jobType via the
     * annotation attribute.
     */
    public static class CustomJobTypeHandlerWithLongClassName implements JobRequestHandler<CustomJobTypeRequest> {
        @ConfigurableJob(name = "My Display Name", jobType = "ShortType")
        @Override
        public void run(CustomJobTypeRequest request) {
            // Test implementation
        }
    }

    /**
     * Job request for the too-long handler simple-name test.
     */
    public record TooLongRequest() implements JobRequest {
        @Override
        public Class<HandlerWithClassNameExceedingJobTypeBudgetAndThenSome> getJobRequestHandler() {
            return HandlerWithClassNameExceedingJobTypeBudgetAndThenSome.class;
        }
    }

    /**
     * Handler whose simple class name intentionally exceeds the 37-character jobType budget
     * without an override — exercises the length validator.
     */
    public static class HandlerWithClassNameExceedingJobTypeBudgetAndThenSome
            implements JobRequestHandler<TooLongRequest> {
        @ConfigurableJob
        @Override
        public void run(TooLongRequest request) {
            // Test implementation
        }
    }

    /**
     * Job request for {@link InvalidCharJobTypeHandler}.
     */
    public record InvalidCharRequest() implements JobRequest {
        @Override
        public Class<InvalidCharJobTypeHandler> getJobRequestHandler() {
            return InvalidCharJobTypeHandler.class;
        }
    }

    /**
     * Handler that attempts to use characters outside [A-Za-z0-9_-] in its jobType override.
     */
    public static class InvalidCharJobTypeHandler implements JobRequestHandler<InvalidCharRequest> {
        @ConfigurableJob(jobType = "bad type!")
        @Override
        public void run(InvalidCharRequest request) {
            // Test implementation
        }
    }

    /**
     * Job request paired with {@link FirstHandlerWithSharedOverride}.
     */
    public record FirstSharedRequest() implements JobRequest {
        @Override
        public Class<FirstHandlerWithSharedOverride> getJobRequestHandler() {
            return FirstHandlerWithSharedOverride.class;
        }
    }

    /**
     * First handler declaring the shared jobType override — triggers the uniqueness check
     * together with {@link SecondHandlerWithSharedOverride}.
     */
    public static class FirstHandlerWithSharedOverride implements JobRequestHandler<FirstSharedRequest> {
        @ConfigurableJob(jobType = "SharedOverride")
        @Override
        public void run(FirstSharedRequest request) {
            // Test implementation
        }
    }

    /**
     * Job request paired with {@link SecondHandlerWithSharedOverride}.
     */
    public record SecondSharedRequest() implements JobRequest {
        @Override
        public Class<SecondHandlerWithSharedOverride> getJobRequestHandler() {
            return SecondHandlerWithSharedOverride.class;
        }
    }

    /**
     * Second handler intentionally reusing the same jobType override as
     * {@link FirstHandlerWithSharedOverride} to prove the collision detector operates on the
     * effective jobType, not on the handler simple name.
     */
    public static class SecondHandlerWithSharedOverride implements JobRequestHandler<SecondSharedRequest> {
        @ConfigurableJob(jobType = "SharedOverride")
        @Override
        public void run(SecondSharedRequest request) {
            // Test implementation
        }
    }
}

package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSet;
import ch.css.jobrunr.control.domain.JobParameterType;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ParameterExtractor.
 * Verifies extraction of both inline and external parameters with proper validation.
 */
class ParameterExtractorTest {

    private IndexView index;
    private ParameterExtractor extractor;

    @BeforeEach
    void setup() throws IOException {
        Indexer indexer = new Indexer();
        // Index all test classes and dependencies
        indexClass(indexer, JobParameterDefinition.class);
        indexClass(indexer, JobParameterSet.class);
        indexClass(indexer, InlineParametersRecord.class);
        indexClass(indexer, ExternalParametersRecord.class);
        indexClass(indexer, MixedTypesRecord.class);
        indexClass(indexer, EnumParameterRecord.class);
        indexClass(indexer, MultiEnumParameterRecord.class);
        indexClass(indexer, CustomNameParametersRecord.class);
        indexClass(indexer, MultilineParameterRecord.class);
        indexClass(indexer, TestEnum.class);
        indexClass(indexer, EmptyParameterSetRecord.class);
        indexClass(indexer, MissingNameInParameterSetRecord.class);
        indexClass(indexer, MissingTypeInParameterSetRecord.class);
        index = indexer.complete();
        extractor = new ParameterExtractor(index);
    }

    @Test
    void shouldExtractInlineParametersFromRecordComponents() {
        ClassInfo recordClass = index.getClassByName(InlineParametersRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertFalse(result.usesExternalParameters());
        assertNull(result.parameterSetFieldName());
        assertEquals(2, result.parameters().size());

        // Check parameters by name (order not guaranteed)
        var nameParam = result.parameters().stream()
                .filter(p -> p.name().equals("name"))
                .findFirst()
                .orElseThrow();
        assertEquals("name", nameParam.name());
        assertEquals(JobParameterType.STRING, nameParam.type());
        assertTrue(nameParam.required());
        assertNull(nameParam.defaultValue());

        var countParam = result.parameters().stream()
                .filter(p -> p.name().equals("count"))
                .findFirst()
                .orElseThrow();
        assertEquals("count", countParam.name());
        assertEquals(JobParameterType.INTEGER, countParam.type());
        assertTrue(countParam.required());
    }

    @Test
    void shouldExtractExternalParameters() {
        ClassInfo recordClass = index.getClassByName(ExternalParametersRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertTrue(result.usesExternalParameters());
        assertNull(result.parameterSetFieldName());
        assertEquals(2, result.parameters().size());

        var param1 = result.parameters().getFirst();
        assertEquals("externalParam1", param1.name());
        assertEquals(JobParameterType.STRING, param1.type());
        assertTrue(param1.required());

        var param2 = result.parameters().get(1);
        assertEquals("externalParam2", param2.name());
        assertEquals(JobParameterType.INTEGER, param2.type());
        assertFalse(param2.required());
        assertEquals("42", param2.defaultValue());
    }

    @Test
    void shouldHandleMixedParameterTypes() {
        ClassInfo recordClass = index.getClassByName(MixedTypesRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertEquals(6, result.parameters().size());

        // Find and verify each parameter by name
        var stringParam = findParam(result, "stringParam");
        assertEquals(JobParameterType.STRING, stringParam.type());

        var intParam = findParam(result, "intParam");
        assertEquals(JobParameterType.INTEGER, intParam.type());

        var boolParam = findParam(result, "boolParam");
        assertEquals(JobParameterType.BOOLEAN, boolParam.type());

        var dateParam = findParam(result, "dateParam");
        assertEquals(JobParameterType.DATE, dateParam.type());

        var dateTimeParam = findParam(result, "dateTimeParam");
        assertEquals(JobParameterType.DATETIME, dateTimeParam.type());

        var longParam = findParam(result, "longParam");
        assertEquals(JobParameterType.INTEGER, longParam.type()); // Long mapped to INTEGER
    }

    @Test
    void shouldExtractEnumValues() {
        ClassInfo recordClass = index.getClassByName(EnumParameterRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertEquals(1, result.parameters().size());

        var param = result.parameters().getFirst();
        assertEquals("status", param.name());
        assertEquals(JobParameterType.ENUM, param.type());
        assertEquals(3, param.enumValues().size());
        assertTrue(param.enumValues().contains("PENDING"));
        assertTrue(param.enumValues().contains("ACTIVE"));
        assertTrue(param.enumValues().contains("COMPLETED"));
    }

    @Test
    void shouldExtractMultiEnumValues() {
        ClassInfo recordClass = index.getClassByName(MultiEnumParameterRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertEquals(1, result.parameters().size());

        var param = result.parameters().getFirst();
        assertEquals("statuses", param.name());
        assertEquals(JobParameterType.MULTI_ENUM, param.type());
        assertEquals(3, param.enumValues().size());
    }

    @Test
    void shouldRespectCustomParameterNames() {
        ClassInfo recordClass = index.getClassByName(CustomNameParametersRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertEquals(2, result.parameters().size());

        var param1 = result.parameters().getFirst();
        assertEquals("Custom Name", param1.name());
        assertEquals("default value", param1.defaultValue());
        assertFalse(param1.required());

        var param2 = result.parameters().get(1);
        assertEquals("Another Custom Name", param2.name());
        assertTrue(param2.required());
    }

    @Test
    void shouldHandleMultilineType() {
        ClassInfo recordClass = index.getClassByName(MultilineParameterRecord.class.getName());

        ParameterExtractor.AnalyzedParameters result = extractor.analyzeRecordParameters(recordClass);

        assertEquals(1, result.parameters().size());

        var param = result.parameters().getFirst();
        assertEquals("description", param.name());
        assertEquals(JobParameterType.MULTILINE, param.type());
    }

    @Test
    void shouldThrowExceptionForEmptyParameterSet() {
        ClassInfo recordClass = index.getClassByName(EmptyParameterSetRecord.class.getName());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> extractor.analyzeRecordParameters(recordClass));

        assertTrue(exception.getMessage().contains("must define at least one parameter"));
    }

    @Test
    void shouldThrowExceptionForMissingParameterName() {
        ClassInfo recordClass = index.getClassByName(MissingNameInParameterSetRecord.class.getName());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> extractor.analyzeRecordParameters(recordClass));

        assertTrue(exception.getMessage().contains("must have a non-empty 'name' attribute"));
    }

    @Test
    void shouldThrowExceptionForMissingParameterType() {
        ClassInfo recordClass = index.getClassByName(MissingTypeInParameterSetRecord.class.getName());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> extractor.analyzeRecordParameters(recordClass));

        assertTrue(exception.getMessage().contains("must have a 'type' attribute"));
    }

    // Test classes and enums

    private ch.css.jobrunr.control.domain.JobParameter findParam(ParameterExtractor.AnalyzedParameters result, String name) {
        return result.parameters().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Parameter not found: " + name));
    }

    public enum TestEnum {
        PENDING, ACTIVE, COMPLETED
    }

    public record InlineParametersRecord(String name, Integer count) {
    }

    @JobParameterSet({
            @JobParameterDefinition(name = "externalParam1", type = "java.lang.String"),
            @JobParameterDefinition(name = "externalParam2", type = "java.lang.Integer", defaultValue = "42")
    })
    public record ExternalParametersRecord() {
    }

    public record MixedTypesRecord(
            String stringParam,
            Integer intParam,
            Boolean boolParam,
            LocalDate dateParam,
            LocalDateTime dateTimeParam,
            Long longParam
    ) {
    }

    public record EnumParameterRecord(TestEnum status) {
    }

    public record MultiEnumParameterRecord(EnumSet<TestEnum> statuses) {
    }

    public record CustomNameParametersRecord(
            @JobParameterDefinition(name = "Custom Name", defaultValue = "default value")
            String field1,
            @JobParameterDefinition(name = "Another Custom Name")
            Integer field2
    ) {
    }

    public record MultilineParameterRecord(
            @JobParameterDefinition(type = "MULTILINE")
            String description
    ) {
    }

    @JobParameterSet({})
    public record EmptyParameterSetRecord() {
    }

    @JobParameterSet({
            @JobParameterDefinition(type = "java.lang.String")
    })
    public record MissingNameInParameterSetRecord() {
    }

    @JobParameterSet({
            @JobParameterDefinition(name = "param")
    })
    public record MissingTypeInParameterSetRecord() {
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

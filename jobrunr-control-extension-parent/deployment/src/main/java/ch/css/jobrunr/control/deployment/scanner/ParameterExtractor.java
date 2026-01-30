package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSet;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts job parameters from record components or fields.
 * Handles both inline parameters (from record components) and external parameters (from @JobParameterSet).
 */
public class ParameterExtractor {

    private static final Logger log = Logger.getLogger(ParameterExtractor.class);

    private static final DotName JOB_PARAMETER_SET = DotName.createSimple(JobParameterSet.class.getName());

    private final IndexView index;

    public ParameterExtractor(IndexView index) {
        this.index = index;
    }

    /**
     * Analyzes record parameters and returns both the parameters and metadata about external parameter usage.
     */
    public AnalyzedParameters analyzeRecordParameters(ClassInfo recordClass) {
        List<JobParameter> parameters = new ArrayList<>();
        RecordComponentInfo parameterSetComponent = null;

        // Step 1: Scan record components for @JobParameterSet annotation
        List<RecordComponentInfo> components = recordClass.recordComponents();
        for (RecordComponentInfo component : components) {
            if (component.hasAnnotation(JOB_PARAMETER_SET)) {
                // Validation: Only one @JobParameterSet allowed
                if (parameterSetComponent != null) {
                    throw new IllegalStateException(
                            "JobRequest " + recordClass.name() +
                                    " has multiple @JobParameterSet annotations on components '" +
                                    parameterSetComponent.name() + "' and '" + component.name() + "'. Only one is allowed.");
                }

                // Validation: Must be String type
                if (!component.type().name().toString().equals("java.lang.String")) {
                    throw new IllegalStateException(
                            "JobRequest " + recordClass.name() +
                                    " component '" + component.name() + "' has @JobParameterSet but is not of type String. " +
                                    "Found: " + component.type().name());
                }

                parameterSetComponent = component;
            }
        }

        // Step 2: Extract parameters based on strategy
        if (parameterSetComponent != null) {
            // EXTERNAL PARAMETERS: Extract from @JobParameterSet annotation
            parameters = extractExternalParameters(parameterSetComponent, recordClass);

            log.infof("Analyzed %s external parameters from @JobParameterSet on component '%s' for job '%s'",
                    parameters.size(), parameterSetComponent.name(), recordClass.simpleName());
            for (JobParameter param : parameters) {
                log.infof("   - External parameter: %s (type=%s, required=%s, default='%s')",
                        param.name(), param.type(), param.required(), param.defaultValue());
            }

            return new AnalyzedParameters(
                    parameters,
                    true, // usesExternalParameters
                    parameterSetComponent.name()
            );

        } else {
            // INLINE PARAMETERS: Scan all record components
            for (RecordComponentInfo component : components) {
                JobParameter parameter = analyzeRecordComponent(component);
                parameters.add(parameter);
            }

            log.debugf("Analyzed %s inline parameters from record components", parameters.size());

            return new AnalyzedParameters(
                    parameters,
                    false, // usesExternalParameters
                    null   // no parameter set field
            );
        }
    }

    /**
     * Extracts parameters from @JobParameterSet annotation.
     */
    private List<JobParameter> extractExternalParameters(RecordComponentInfo parameterSetComponent, ClassInfo recordClass) {
        List<JobParameter> parameters = new ArrayList<>();
        AnnotationInstance annotation = parameterSetComponent.annotation(JOB_PARAMETER_SET);
        AnnotationValue valueArray = annotation.value();

        if (valueArray == null || valueArray.asNestedArray().length == 0) {
            throw new IllegalStateException(
                    "JobRequest " + recordClass.name() +
                            " @JobParameterSet on component '" + parameterSetComponent.name() +
                            "' must define at least one parameter");
        }

        AnnotationInstance[] definitions = valueArray.asNestedArray();

        for (AnnotationInstance defAnnotation : definitions) {
            parameters.add(extractParameterFromDefinition(defAnnotation, recordClass.name().toString()));
        }

        return parameters;
    }

    /**
     * Extracts a JobParameter from a @JobParameterDefinition within @JobParameterSet.
     */
    private JobParameter extractParameterFromDefinition(AnnotationInstance defAnnotation, String jobRequestName) {
        String name = getAnnotationValue(defAnnotation, "name", "");
        String defaultValue = getAnnotationValue(defAnnotation, "defaultValue", JobParameterDefinition.NO_DEFAULT_VALUE);
        String typeString = getAnnotationValue(defAnnotation, "type", "");

        if (name.isEmpty()) {
            throw new IllegalStateException(
                    "Parameter definition in @JobParameterSet of JobRequest " + jobRequestName +
                            " must have a non-empty 'name' attribute");
        }

        if (typeString.isEmpty()) {
            throw new IllegalStateException(
                    "Parameter '" + name + "' in @JobParameterSet of JobRequest " + jobRequestName +
                            " must have a 'type' attribute specifying the fully qualified type name");
        }

        // Determine type
        JobParameterType parameterType = mapStringToParameterType(typeString);
        List<String> enumValues = List.of();

        // If enum, extract values
        if (parameterType == JobParameterType.ENUM || parameterType == JobParameterType.MULTI_ENUM) {
            enumValues = extractEnumValuesFromTypeString(typeString, parameterType);
        }

        boolean required = defaultValue.equals(JobParameterDefinition.NO_DEFAULT_VALUE);

        log.debugf("Extracted external parameter: name=%s, type=%s, required=%s", name, parameterType, required);

        return new JobParameter(name, parameterType, required, defaultValue, enumValues);
    }

    /**
     * Analyzes a record component for inline parameter jobs.
     */
    private JobParameter analyzeRecordComponent(RecordComponentInfo component) {
        String componentName = component.name();
        Type componentType = component.type();

        AnnotationInstance jobParamAnnotation = component.annotation(JobParameterDefinition.class);

        String name = componentName;
        String defaultValue = null;
        boolean required = true;
        JobParameterType parameterType = null;

        if (jobParamAnnotation != null) {
            // Extract name if specified
            AnnotationValue nameValue = jobParamAnnotation.value("name");
            if (nameValue != null && !nameValue.asString().isEmpty()) {
                name = nameValue.asString();
            }

            // Extract default value if specified
            AnnotationValue defaultValueAnnotation = jobParamAnnotation.value("defaultValue");
            if (defaultValueAnnotation != null && !defaultValueAnnotation.asString().equals(JobParameterDefinition.NO_DEFAULT_VALUE)) {
                defaultValue = defaultValueAnnotation.asString();
                required = false;
            }

            // Check for explicit type override (e.g., "MULTILINE")
            AnnotationValue typeValue = jobParamAnnotation.value("type");
            if (typeValue != null && !typeValue.asString().isEmpty()) {
                String typeString = typeValue.asString();
                parameterType = mapStringToParameterType(typeString);
                log.debugf("Using explicit type '%s' for inline parameter '%s'", typeString, name);
            }
        }

        // If no explicit type, map Java type to JobParameterType
        if (parameterType == null) {
            parameterType = mapToJobParameterType(componentType);
        }

        List<String> enumValues = getEnumValuesIfApplicable(componentType, parameterType);
        return new JobParameter(name, parameterType, required, defaultValue, enumValues);
    }

    /**
     * Maps a type string (from @JobParameterDefinition.type) to JobParameterType.
     */
    private JobParameterType mapStringToParameterType(String typeString) {
        return switch (typeString) {
            case "java.lang.String" -> JobParameterType.STRING;
            case "MULTILINE" -> JobParameterType.MULTILINE;
            case "java.lang.Integer", "int" -> JobParameterType.INTEGER;
            case "java.lang.Long", "long" -> JobParameterType.INTEGER;
            case "java.lang.Boolean", "boolean" -> JobParameterType.BOOLEAN;
            case "java.time.LocalDate" -> JobParameterType.DATE;
            case "java.time.LocalDateTime" -> JobParameterType.DATETIME;
            default -> {
                // Check if EnumSet<T>
                if (typeString.startsWith("java.util.EnumSet<") && typeString.endsWith(">")) {
                    yield JobParameterType.MULTI_ENUM;
                }

                // Check if it's an enum class
                DotName typeDotName = DotName.createSimple(typeString);
                ClassInfo classInfo = index.getClassByName(typeDotName);
                if (classInfo != null && classInfo.isEnum()) {
                    yield JobParameterType.ENUM;
                }

                throw new IllegalStateException(
                        "Unsupported parameter type: " + typeString + ". " +
                                "Supported types: String, MULTILINE, Integer, Long, Boolean, LocalDate, LocalDateTime, Enum, EnumSet<Enum>");
            }
        };
    }

    /**
     * Maps a Jandex Type to JobParameterType.
     */
    private JobParameterType mapToJobParameterType(Type type) {
        if (type == null || type.name() == null) {
            log.debugf("Type is null or has no name; defaulting to STRING");
            return JobParameterType.STRING;
        }

        // Check for EnumSet<T>
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType paramType = type.asParameterizedType();
            String paramTypeName = paramType.name().toString();
            if ("java.util.EnumSet".equals(paramTypeName)) {
                log.debugf("Detected EnumSet type, mapping to MULTI_ENUM");
                return JobParameterType.MULTI_ENUM;
            }
        }

        String typeName = type.name().toString();

        // Handle primitive kinds
        if (type.kind() == Type.Kind.PRIMITIVE) {
            switch (typeName) {
                case "int", "I" -> typeName = "int";
                case "boolean", "Z" -> typeName = "boolean";
                case "long", "J" -> typeName = "long";
                case "double", "D" -> typeName = "double";
                case "float", "F" -> typeName = "float";
                case "short", "S" -> typeName = "short";
                case "byte", "B" -> typeName = "byte";
                case "char", "C" -> typeName = "char";
            }
        }

        JobParameterType resolved = switch (typeName) {
            case "java.lang.String", "String" -> JobParameterType.STRING;
            case "java.lang.Integer", "Integer", "int" -> JobParameterType.INTEGER;
            case "java.lang.Boolean", "Boolean", "boolean" -> JobParameterType.BOOLEAN;
            case "java.lang.Long", "Long", "long" -> JobParameterType.INTEGER;
            case "java.lang.Double", "Double", "double" -> JobParameterType.INTEGER;
            case "java.lang.Float", "Float", "float" -> JobParameterType.INTEGER;
            case "java.lang.Short", "Short", "short" -> JobParameterType.INTEGER;
            case "java.lang.Byte", "Byte", "byte" -> JobParameterType.INTEGER;
            case "java.lang.Character", "Character", "char" -> JobParameterType.STRING;
            case "java.time.LocalDate" -> JobParameterType.DATE;
            case "java.time.LocalDateTime" -> JobParameterType.DATETIME;
            default -> {
                if (type.kind() == Type.Kind.CLASS) {
                    yield JobParameterType.ENUM;
                }
                yield JobParameterType.STRING;
            }
        };

        log.debugf("Mapped type '%s' to JobParameterType.%s", typeName, resolved.name());
        return resolved;
    }

    /**
     * Extracts enum values from a type string.
     */
    private List<String> extractEnumValuesFromTypeString(String typeString, JobParameterType parameterType) {
        String enumClassName = typeString;

        // For EnumSet, extract the inner type
        if (parameterType == JobParameterType.MULTI_ENUM && typeString.startsWith("java.util.EnumSet<") && typeString.endsWith(">")) {
            enumClassName = typeString.substring("java.util.EnumSet<".length(), typeString.length() - 1);
        }

        DotName enumDotName = DotName.createSimple(enumClassName);
        ClassInfo enumClassInfo = index.getClassByName(enumDotName);

        if (enumClassInfo == null) {
            log.warnf("Could not find enum class in index: %s", enumClassName);
            return List.of();
        }

        if (!enumClassInfo.isEnum()) {
            log.warnf("Class %s is not an enum", enumClassName);
            return List.of();
        }

        return extractEnumConstants(enumClassInfo);
    }

    /**
     * Gets enum values if the type is an enum or EnumSet.
     */
    private List<String> getEnumValuesIfApplicable(Type type, JobParameterType parameterType) {
        if (parameterType != JobParameterType.ENUM && parameterType != JobParameterType.MULTI_ENUM) {
            return List.of();
        }

        Type enumType = type;

        // For MULTI_ENUM, extract the type parameter from EnumSet<T>
        if (parameterType == JobParameterType.MULTI_ENUM && type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType paramType = type.asParameterizedType();
            if (!paramType.arguments().isEmpty()) {
                enumType = paramType.arguments().getFirst();
                log.debugf("Extracted enum type from EnumSet: %s", enumType.name());
            }
        }

        if (enumType == null || enumType.kind() != Type.Kind.CLASS) {
            log.debugf("Type is not a CLASS, cannot extract enum values");
            return List.of();
        }

        ClassInfo enumClassInfo = index.getClassByName(enumType.name());
        if (enumClassInfo == null) {
            log.debugf("Could not find class info for type: %s", enumType.name());
            return List.of();
        }

        if (!enumClassInfo.isEnum()) {
            log.debugf("Class %s is not an enum", enumType.name());
            return List.of();
        }

        return extractEnumConstants(enumClassInfo);
    }

    /**
     * Extracts enum constant names from a ClassInfo.
     */
    private List<String> extractEnumConstants(ClassInfo enumClassInfo) {
        List<String> enumValues = new ArrayList<>();
        for (FieldInfo field : enumClassInfo.fields()) {
            if (field.isEnumConstant()) {
                enumValues.add(field.name());
            }
        }

        log.debugf("Extracted %s enum values from %s: %s", enumValues.size(), enumClassInfo.name(), enumValues);
        return enumValues;
    }

    private String getAnnotationValue(AnnotationInstance annotation, String name, String defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asString() : defaultValue;
    }

    /**
     * Helper record to return analysis results including external parameter metadata.
     */
    public record AnalyzedParameters(
            List<JobParameter> parameters,
            boolean usesExternalParameters,
            String parameterSetFieldName
    ) {
    }
}

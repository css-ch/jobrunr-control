package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSectionLayout;
import ch.css.jobrunr.control.annotations.JobParameterSet;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterSection;
import ch.css.jobrunr.control.domain.JobParameterType;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts job parameters from record components or fields.
 * Handles both inline parameters (from record components) and external parameters (from @JobParameterSet).
 */
@SuppressWarnings("java:S1192") // String literals are type names, duplicates are acceptable for clarity
public class ParameterExtractor {

    private static final Logger LOG = Logger.getLogger(ParameterExtractor.class);

    private static final DotName JOB_PARAMETER_SET = DotName.createSimple(JobParameterSet.class.getName());

    private final IndexView index;

    public ParameterExtractor(IndexView index) {
        this.index = index;
    }

    /**
     * Analyzes record parameters and returns both the parameters and metadata about external parameter usage.
     */
    public AnalyzedParameters analyzeRecordParameters(ClassInfo recordClass) {
        AnnotationInstance parameterSetAnnotation = recordClass.annotation(JOB_PARAMETER_SET);

        if (parameterSetAnnotation != null) {
            return analyzeExternalParameters(parameterSetAnnotation, recordClass);
        } else {
            return analyzeInlineParameters(recordClass.recordComponents());
        }
    }

    /**
     * Analyzes external parameters from @JobParameterSet on the record type.
     */
    private AnalyzedParameters analyzeExternalParameters(AnnotationInstance parameterSetAnnotation, ClassInfo recordClass) {
        AnnotationValue parameterSetClassValue = parameterSetAnnotation.value("parameterSetClass");
        if (parameterSetClassValue != null && !parameterSetClassValue.asString().isEmpty()) {
            Type parameterSetType = parameterSetClassValue.asClass();
            ClassInfo parameterSetClass = index.getClassByName(parameterSetType.name());
            if(parameterSetClass != null) {
                LOG.debugf("JobRequest '%s' uses external parameter set class: %s", recordClass.simpleName(), parameterSetType.name());
                AnalyzedParameters analyzedParameters = analyzeInlineParameters(parameterSetClass.recordComponents());
                return new AnalyzedParameters(
                        analyzedParameters.parameters(),
                        analyzedParameters.parameterSections(),
                        true,
                        parameterSetType.name().toString()
                );
            }
        }
        throw new IllegalStateException(
                "JobRequest " + recordClass.name() +
                        " has @JobParameterSet annotation but 'parameterSetClass' attribute is missing, empty or class can not be loaded");
    }

    /**
     * Analyzes inline parameters from record components.
     */
    private AnalyzedParameters analyzeInlineParameters(List<RecordComponentInfo> components) {
        List<JobParameter> parameters = new ArrayList<>();
        List<JobParameterSection> parameterSections = new ArrayList<>();

        int order = 9999;
        for (RecordComponentInfo component : components) {
            JobParameter parameter = analyzeRecordComponent(component, order++);
            JobParameterSection parameterSection = analyzeRecordComponentSection(component, order);
            if (parameterSection != null) {
                parameterSections.add(parameterSection);
            }
            parameters.add(parameter);
        }
        for(JobParameter parameter : parameters) {
            if(parameterSections.stream().noneMatch(s -> s.id().equals(parameter.sectionId()))) {
                if(parameter.sectionId().equals("default")) {
                    parameterSections.add(new JobParameterSection("default", null, 9999, JobParameterSectionLayout.SINGLE_VALUE_ON_LINE_LABEL_OBOVE));
                } else {
                    LOG.warnf("Parameter '%s' references sectionId '%s' which does not exist. Consider adding a @JobParameterSection with id '%s'.",
                            parameter.name(), parameter.sectionId(), parameter.sectionId());
                }
            }
        }

        LOG.debugf("Analyzed %s inline parameters from record components. %s Sections", parameters.size(), parameterSections.size());

        return new AnalyzedParameters(
                parameters,
                parameterSections,
                false,
                null
        );
    }

    /**
     * Analyzes a record component for inline parameter jobs.
     */
    private JobParameter analyzeRecordComponent(RecordComponentInfo component, int order) {
        String componentName = component.name();
        Type componentType = component.type();

        AnnotationInstance jobParamAnnotation = component.annotation(JobParameterDefinition.class);

        String name = componentName;
        String displayName = componentName;
        String description = null;
        String sectionId = "default";
        String defaultValue = null;
        boolean required = true;
        JobParameterType parameterType = null;

        if (jobParamAnnotation != null) {
            // Extract name if specified
            AnnotationValue nameValue = jobParamAnnotation.value("name");
            if (nameValue != null && !nameValue.asString().isEmpty()) {
                name = nameValue.asString();
            }

            // Extract name if specified
            AnnotationValue displayNameValue = jobParamAnnotation.value("displayName");
            if (displayNameValue != null && !displayNameValue.asString().isEmpty()) {
                displayName = displayNameValue.asString();
            }

            // Extract name if specified
            AnnotationValue descriptionValue = jobParamAnnotation.value("description");
            if (descriptionValue != null && !descriptionValue.asString().isEmpty()) {
                description = descriptionValue.asString();
            }

            // Extract name if specified
            AnnotationValue sectionIdValue = jobParamAnnotation.value("sectionId");
            if (sectionIdValue != null && !sectionIdValue.asString().isEmpty()) {
                sectionId = sectionIdValue.asString();
            }

            // Extract name if specified
            AnnotationValue orderValue = jobParamAnnotation.value("order");
            if (orderValue != null) {
                order = orderValue.asInt();
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
                LOG.debugf("Using explicit type '%s' for inline parameter '%s'", typeString, name);
            }
        }

        // If no explicit type, map Java type to JobParameterType
        if (parameterType == null) {
            parameterType = mapToJobParameterType(componentType);
        }

        List<String> enumValues = getEnumValuesIfApplicable(componentType, parameterType);
        return new JobParameter(name, displayName, description, parameterType, required, defaultValue, enumValues, order, sectionId);
    }

    /**
     * Analyzes a record component for @JobParameterSection metadata.
     */
    private JobParameterSection analyzeRecordComponentSection(RecordComponentInfo component, int order) {
        AnnotationInstance jobParamSectionAnnotation = component.annotation(ch.css.jobrunr.control.annotations.JobParameterSection.class);
        if(jobParamSectionAnnotation != null) {
            String id = null;
            String title = null;
            JobParameterSectionLayout layout = JobParameterSectionLayout.SINGLE_VALUE_ON_LINE_LABEL_OBOVE;

            // Extract id if specified
            AnnotationValue idValue = jobParamSectionAnnotation.value("id");
            if (idValue != null && !idValue.asString().isEmpty()) {
                id = idValue.asString();
            }

            // Extract id if specified
            AnnotationValue titleValue = jobParamSectionAnnotation.value("title");
            if (titleValue != null && !titleValue.asString().isEmpty()) {
                title = titleValue.asString();
            }

            // Extract name if specified
            AnnotationValue orderValue = jobParamSectionAnnotation.value("order");
            if (orderValue != null) {
                order = orderValue.asInt();
            }

            // Extract name if specified
            AnnotationValue layoutValue = jobParamSectionAnnotation.value("layout");
            if (layoutValue != null) {
                layout = JobParameterSectionLayout.valueOf(layoutValue.asEnum());
            }

            return new JobParameterSection(id, title, order, layout);
        }
        return null;
    }

    /**
     * Maps a type string (from @JobParameterDefinition.type) to JobParameterType.
     */
    private JobParameterType mapStringToParameterType(String typeString) {
        return switch (typeString) {
            case "java.lang.String" -> JobParameterType.STRING;
            case "MULTILINE" -> JobParameterType.MULTILINE;
            case "java.lang.Integer", "int", "java.lang.Long", "long" -> JobParameterType.INTEGER;
            case "java.lang.Double", "double", "java.lang.Float", "float" -> JobParameterType.DOUBLE;
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
                                "Supported types: String, MULTILINE, Integer, Long, Double, Float, Boolean, LocalDate, LocalDateTime, Enum, EnumSet<Enum>");
            }
        };
    }

    /**
     * Maps a Jandex Type to JobParameterType.
     */
    private JobParameterType mapToJobParameterType(Type type) {
        if (type == null || type.name() == null) {
            LOG.debugf("Type is null or has no name; defaulting to STRING");
            return JobParameterType.STRING;
        }

        // Check for EnumSet<T>
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType paramType = type.asParameterizedType();
            String paramTypeName = paramType.name().toString();
            if ("java.util.EnumSet".equals(paramTypeName)) {
                LOG.debugf("Detected EnumSet type, mapping to MULTI_ENUM");
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
                default -> {
                    // Unknown primitive type, keep original name
                }
            }
        }

        JobParameterType resolved = switch (typeName) {
            case "java.lang.String", "String" -> JobParameterType.STRING;
            case "java.lang.Integer", "Integer", "int" -> JobParameterType.INTEGER;
            case "java.lang.Boolean", "Boolean", "boolean" -> JobParameterType.BOOLEAN;
            case "java.lang.Long", "Long", "long" -> JobParameterType.INTEGER;
            case "java.lang.Double", "Double", "double" -> JobParameterType.DOUBLE;
            case "java.lang.Float", "Float", "float" -> JobParameterType.DOUBLE;
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

        LOG.debugf("Mapped type '%s' to JobParameterType.%s", typeName, resolved.name());
        return resolved;
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
                LOG.debugf("Extracted enum type from EnumSet: %s", enumType.name());
            }
        }

        if (enumType == null || enumType.kind() != Type.Kind.CLASS) {
            LOG.debugf("Type is not a CLASS, cannot extract enum values");
            return List.of();
        }

        ClassInfo enumClassInfo = index.getClassByName(enumType.name());
        if (enumClassInfo == null) {
            LOG.debugf("Could not find class info for type: %s", enumType.name());
            return List.of();
        }

        if (!enumClassInfo.isEnum()) {
            LOG.debugf("Class %s is not an enum", enumType.name());
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

        LOG.debugf("Extracted %s enum values from %s: %s", enumValues.size(), enumClassInfo.name(), enumValues);
        return enumValues;
    }

    /**
     * Helper record to return analysis results including external parameter metadata.
     */
    public record AnalyzedParameters(
            List<JobParameter> parameters,
            List<JobParameterSection> parameterSections,
            boolean usesExternalParameters,
            String externalParametersClassName
    ) {
    }
}

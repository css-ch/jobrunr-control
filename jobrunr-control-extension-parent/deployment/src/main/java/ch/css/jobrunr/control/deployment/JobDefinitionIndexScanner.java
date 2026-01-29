package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.annotations.JobParameterSet;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import ch.css.jobrunr.control.domain.JobSettings;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Scans the Jandex index for JobRequestHandler implementations annotated with @ConfigurableJob
 * and extracts job definitions.
 */
class JobDefinitionIndexScanner {

    private static final Logger log = Logger.getLogger(JobDefinitionIndexScanner.class);

    private static final DotName JOB_REQUEST_HANDLER = DotName.createSimple(JobRequestHandler.class.getName());
    private static final DotName JOB_REQUEST = DotName.createSimple(JobRequest.class.getName());
    private static final DotName JOB_PARAMETER_SET = DotName.createSimple(JobParameterSet.class.getName());

    private JobDefinitionIndexScanner() {
        // Helper
    }

    public static Set<JobDefinition> findJobSpecifications(IndexView index) {
        Set<JobDefinition> jobDefinitions = new java.util.HashSet<>();
        log.debugf("Searching for JobRequestHandler implementations");
        // Find all classes that implement JobRequestHandler
        var implementations = index.getAllKnownImplementations(JOB_REQUEST_HANDLER);

        for (ClassInfo classInfo : implementations) {
            log.debugf("Inspecting implementation: %s", classInfo.name());
            // Check if the class has a method annotated with @ConfigurableJob
            MethodInfo runMethod = findRunMethodWithConfigurableJobAnnotation(classInfo);
            if (runMethod != null) {
                log.debugf("Found @ConfigurableJob run method on %s", classInfo.name());
                // Extract the type parameter from JobRequestHandler
                Type jobRequestType = findParameterizedInterfaceArgument(classInfo, index, JOB_REQUEST_HANDLER);
                if (jobRequestType != null && jobRequestType.kind() == Type.Kind.CLASS) {
                    log.debugf("JobRequest type resolved: %s", jobRequestType.name());
                    ClassInfo requestClassInfo = index.getClassByName(jobRequestType.name());
                    if (requestClassInfo != null && isRecord(requestClassInfo) && implementsInterfaceRecursive(requestClassInfo, index, JOB_REQUEST)) {
                        // Extract job type and batch flag
                        String jobType = classInfo.simpleName();
                        boolean isBatchJob = getBatchJobFlag(runMethod);

                        // Analyze the record parameters
                        AnalyzedParameters analyzedParams = analyzeRecordParameters(requestClassInfo, index);

                        // Extract JobSettings from annotation
                        JobSettings jobSettings = extractJobSettings(runMethod);

                        // Extract field names from parameters
                        log.infof("Discovered job: %s (batch=%s, externalParams=%s) with %s parameters",
                                jobType, isBatchJob, analyzedParams.usesExternalParameters(), analyzedParams.parameters().size());

                        jobDefinitions.add(new JobDefinition(
                                jobType,
                                isBatchJob,
                                requestClassInfo.name().toString(),
                                classInfo.name().toString(), // handlerClassName
                                analyzedParams.parameters(),
                                isRecord(requestClassInfo),
                                jobSettings,
                                analyzedParams.usesExternalParameters(),
                                analyzedParams.parameterSetFieldName()
                        ));
                    } else {
                        log.debugf("Request class %s is not a valid record JobRequest or does not implement JobRequest", jobRequestType.name());
                    }
                } else {
                    log.debugf("Could not resolve parameterized JobRequest type");
                }
            }
        }
        return jobDefinitions;
    }

    private static MethodInfo findRunMethodWithConfigurableJobAnnotation(ClassInfo classInfo) {
        for (MethodInfo method : classInfo.methods()) {
            log.debugf("Search @ConfigurableJob run method on %s:%s", classInfo.name(), method.name());
            log.debugf("   Annotations are %s", classInfo.annotations());
            log.debugf("   Parameters are %s", method.name().equals("run"));

            if (method.name().equals("run") && method.hasAnnotation(ConfigurableJob.class)) {
                log.debugf("Method %s#%s annotated with @ConfigurableJob", classInfo.name(), method.name());
                return method;
            }
        }
        return null;
    }

    // New helper: find the first type argument for a parameterized interface anywhere in the
    // class/interface hierarchy (searches implemented interfaces and superclasses).
    private static Type findParameterizedInterfaceArgument(ClassInfo classInfo, IndexView index, DotName targetInterface) {
        // Check interfaces on this class
        for (Type interfaceType : classInfo.interfaceTypes()) {
            log.debugf(">> Inspecting interface: %s", interfaceType.name());
            if (interfaceType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                ParameterizedType pt = interfaceType.asParameterizedType();
                if (pt.name().equals(targetInterface) && !pt.arguments().isEmpty()) {
                    log.debugf("Found parameterized interface %s on %s -> argument %s", targetInterface, classInfo.name(), pt.arguments().getFirst().name());
                    return pt.arguments().getFirst();
                }

                // Check if this parameterized interface extends/implements the target interface
                // e.g., ConfigurableJob<T> extends JobRequestHandler<T>
                ClassInfo ifaceInfo = index.getClassByName(pt.name());
                if (ifaceInfo != null && implementsInterfaceRecursive(ifaceInfo, index, targetInterface)) {
                    // This interface extends the target, so return its type argument
                    if (!pt.arguments().isEmpty()) {
                        log.debugf("Found parameterized interface %s (which extends %s) on %s -> argument %s",
                                pt.name(), targetInterface, classInfo.name(), pt.arguments().getFirst().name());
                        return pt.arguments().getFirst();
                    }
                }
            } else if (interfaceType.name().equals(targetInterface)) {
                // raw type found, no type argument
                log.debugf("Found raw interface %s on %s", targetInterface, classInfo.name());
            }

            // Recurse into the interface's declaration (in case it extends other interfaces)
            ClassInfo ifaceInfo = index.getClassByName(interfaceType.name());
            if (ifaceInfo != null) {
                Type found = findParameterizedInterfaceArgument(ifaceInfo, index, targetInterface);
                if (found != null) {
                    return found;
                }
            }
        }

        // Check superclass
        if (classInfo.superName() != null) {
            ClassInfo superClass = index.getClassByName(classInfo.superName());
            if (superClass != null) {
                return findParameterizedInterfaceArgument(superClass, index, targetInterface);
            }
        }

        return null;
    }

    // New helper: checks whether the class or any of its superclasses/interfaces implement targetInterface
    private static boolean implementsInterfaceRecursive(ClassInfo classInfo, IndexView index, DotName targetInterface) {
        for (Type interfaceType : classInfo.interfaceTypes()) {
            if (interfaceType.name().equals(targetInterface)) {
                return true;
            }
            ClassInfo ifaceInfo = index.getClassByName(interfaceType.name());
            if (ifaceInfo != null && implementsInterfaceRecursive(ifaceInfo, index, targetInterface)) {
                return true;
            }
        }

        if (classInfo.superName() != null) {
            ClassInfo superClass = index.getClassByName(classInfo.superName());
            return superClass != null && implementsInterfaceRecursive(superClass, index, targetInterface);
        }

        return false;
    }

    private static boolean isRecord(ClassInfo classInfo) {
        // In Jandex, records can be detected by checking if the class is final and has the RECORD access flag
        // The RECORD flag is 0x10 (16 in decimal) according to JVM spec
        // However, Jandex might not expose this directly, so we can also check for record characteristics:
        // - Must be final
        // - Extends java.lang.Record
        // - Or has canonical constructor

        // Check if extends Record
        if (classInfo.superName() != null && classInfo.superName().toString().equals("java.lang.Record")) {
            return true;
        }

        // Fallback: check for RECORD flag (0x10)
        return (classInfo.flags() & 0x10) != 0;
    }

    private static boolean getBatchJobFlag(MethodInfo method) {
        AnnotationInstance annotation = method.annotation(ConfigurableJob.class);
        if (annotation != null) {
            AnnotationValue value = annotation.value("isBatch");
            return value != null && value.asBoolean(); // default is false (check ConfigurableJob)
        }
        return false;
    }

    private static JobSettings extractJobSettings(MethodInfo method) {
        AnnotationInstance annotation = method.annotation(ConfigurableJob.class);
        if (annotation == null) {
            return createDefaultJobSettings();
        }

        // Extract name
        String name = getAnnotationValue(annotation, "name", "");

        // Extract isBatch
        boolean isBatch = getAnnotationValue(annotation, "isBatch", false);

        // Extract retries
        int retries = getAnnotationValue(annotation, "retries", ConfigurableJob.NBR_OF_RETRIES_NOT_PROVIDED);

        // Extract labels
        List<String> labels = getAnnotationValueAsStringList(annotation, "labels");

        // Extract jobFilters as class names
        List<String> jobFilters = getAnnotationValueAsClassNameList(annotation, "jobFilters");

        // Extract queue
        String queue = getAnnotationValue(annotation, "queue", "");

        // Extract runOnServerWithTag
        String runOnServerWithTag = getAnnotationValue(annotation, "runOnServerWithTag", "");

        // Extract mutex
        String mutex = getAnnotationValue(annotation, "mutex", "");

        // Extract rateLimiter
        String rateLimiter = getAnnotationValue(annotation, "rateLimiter", "");

        // Extract processTimeOut
        String processTimeOut = getAnnotationValue(annotation, "processTimeOut", "");

        // Extract deleteOnSuccess
        String deleteOnSuccess = getAnnotationValue(annotation, "deleteOnSuccess", "");

        // Extract deleteOnFailure
        String deleteOnFailure = getAnnotationValue(annotation, "deleteOnFailure", "");

        return new JobSettings(
                name,
                isBatch,
                retries,
                labels,
                jobFilters,
                queue,
                runOnServerWithTag,
                mutex,
                rateLimiter,
                processTimeOut,
                deleteOnSuccess,
                deleteOnFailure
        );
    }

    private static JobSettings createDefaultJobSettings() {
        return new JobSettings(
                "",
                false,
                ConfigurableJob.NBR_OF_RETRIES_NOT_PROVIDED,
                List.of(),
                List.of(),
                "",
                "",
                "",
                "",
                "",
                "",
                ""
        );
    }

    private static String getAnnotationValue(AnnotationInstance annotation, String name, String defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asString() : defaultValue;
    }

    private static boolean getAnnotationValue(AnnotationInstance annotation, String name, boolean defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asBoolean() : defaultValue;
    }

    private static int getAnnotationValue(AnnotationInstance annotation, String name, int defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asInt() : defaultValue;
    }

    private static List<String> getAnnotationValueAsStringList(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        if (value == null) {
            return List.of();
        }
        return List.of(value.asStringArray());
    }

    private static List<String> getAnnotationValueAsClassNameList(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        if (value == null) {
            return List.of();
        }
        // In Jandex, class arrays are represented as Type[]
        Type[] types = value.asClassArray();
        List<String> result = new ArrayList<>();
        for (Type type : types) {
            result.add(type.name().toString());
        }
        return result;
    }

    /**
     * Helper record to return analysis results including external parameter metadata.
     */
    record AnalyzedParameters(
            List<JobParameter> parameters,
            boolean usesExternalParameters,
            String parameterSetFieldName
    ) {
    }

    private static AnalyzedParameters analyzeRecordParameters(ClassInfo recordClass, IndexView index) {
        List<JobParameter> parameters = new ArrayList<>();
        RecordComponentInfo parameterSetComponent = null;

        // Step 1: Scan record components for @JobParameterSet annotation
        // For records, annotations on parameters are stored on record components, not fields
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
                parameters.add(extractParameterFromDefinition(defAnnotation, recordClass.name().toString(), index));
            }

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
            // INLINE PARAMETERS: Existing logic - scan all record components
            for (RecordComponentInfo component : components) {
                JobParameter parameter = analyzeRecordComponent(component, index);
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
     * Extracts a JobParameter from a @JobParameterDefinition within @JobParameterSet.
     * For external parameters, the 'type' attribute is required.
     */
    private static JobParameter extractParameterFromDefinition(
            AnnotationInstance defAnnotation, String jobRequestName, IndexView index) {

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
        JobParameterType parameterType = mapStringToParameterType(typeString, index);
        List<String> enumValues = List.of();

        // If enum, extract values
        if (parameterType == JobParameterType.ENUM || parameterType == JobParameterType.MULTI_ENUM) {
            enumValues = extractEnumValuesFromTypeString(typeString, parameterType, index);
        }

        boolean required = defaultValue.equals(JobParameterDefinition.NO_DEFAULT_VALUE);

        log.debugf("Extracted external parameter: name=%s, type=%s, required=%s", name, parameterType, required);

        return createDomainJobParameter(name, parameterType, required, defaultValue, enumValues);
    }

    /**
     * Maps a type string (from @JobParameterDefinition.type) to JobParameterType.
     */
    private static JobParameterType mapStringToParameterType(String typeString, IndexView index) {
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
     * Extracts enum values from a type string.
     * Handles both "com.example.MyEnum" and "java.util.EnumSet<com.example.MyEnum>".
     */
    private static List<String> extractEnumValuesFromTypeString(String typeString, JobParameterType parameterType, IndexView index) {
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

        // Extract enum constant names
        List<String> enumValues = new ArrayList<>();
        for (FieldInfo field : enumClassInfo.fields()) {
            if (field.isEnumConstant()) {
                enumValues.add(field.name());
            }
        }

        log.debugf("Extracted %s enum values from %s: %s", enumValues.size(), enumClassName, enumValues);
        return enumValues;
    }

    /**
     * Analyzes a record component for inline parameter jobs.
     * This is used for jobs that don't use @JobParameterSet.
     */
    private static JobParameter analyzeRecordComponent(RecordComponentInfo component, IndexView index) {
        String componentName = component.name();
        Type componentType = component.type();

        // Check for @JobParameterDefinition annotation
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
                parameterType = mapStringToParameterType(typeString, index);
                log.debugf("Using explicit type '%s' for inline parameter '%s'", typeString, name);
            }
        }

        // If no explicit type, map Java type to JobParameterType
        if (parameterType == null) {
            parameterType = mapToJobParameterType(componentType);
        }

        List<String> enumValues = getEnumValuesIfApplicable(componentType, parameterType, index);
        // Create and return domain JobParameterDefinition via small factory helper
        return createDomainJobParameter(name, parameterType, required, defaultValue, enumValues);
    }

    private static JobParameter analyzeField(FieldInfo field, IndexView index) {
        String fieldName = field.name();
        Type fieldType = field.type();

        // Check for @JobParameterDefinition annotation
        AnnotationInstance jobParamAnnotation = field.annotation(JobParameterDefinition.class);

        String name = fieldName;
        String defaultValue = null;
        boolean required = true;

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
        }

        // Map Java type to JobParameterType
        JobParameterType parameterType = mapToJobParameterType(fieldType);

        List<String> enumValues = getEnumValuesIfApplicable(fieldType, parameterType, index);
        // Create and return domain JobParameterDefinition via small factory helper
        return createDomainJobParameter(name, parameterType, required, defaultValue, enumValues);
    }

    private static JobParameter createDomainJobParameter(String name, JobParameterType type, boolean required, String defaultValue, List<String> enumValues) {
        // Keep fully-qualified domain class usage to avoid clash with annotation of the same name
        return new JobParameter(name, type, required, defaultValue, enumValues);
    }

    private static JobParameterType mapToJobParameterType(Type type) {
        // Handle null or unknown types early
        if (type == null || type.name() == null) {
            log.debugf("Type is null or has no name; defaulting to STRING");
            return JobParameterType.STRING;
        }

        // Check for EnumSet<T> (parameterized type)
        if (type.kind() == Type.Kind.PARAMETERIZED_TYPE) {
            ParameterizedType paramType = type.asParameterizedType();
            String paramTypeName = paramType.name().toString();
            if ("java.util.EnumSet".equals(paramTypeName)) {
                log.debugf("Detected EnumSet type, mapping to MULTI_ENUM");
                return JobParameterType.MULTI_ENUM;
            }
        }

        // Normalize kind and name handling for primitives and class types
        String typeName = type.name().toString();

        // Handle primitive kinds explicitly (Jandex may represent primitives differently)
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
                default -> { /* leave as-is */ }
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
                // If it's a class type and not matched above, assume enum; otherwise fallback to STRING
                if (type.kind() == Type.Kind.CLASS) {
                    yield JobParameterType.ENUM;
                }
                yield JobParameterType.STRING;
            }
        };

        log.debugf("Mapped type '%s' to JobParameterType.%s", typeName, resolved.name());
        return resolved;
    }

    private static List<String> getEnumValuesIfApplicable(Type type, JobParameterType parameterType, IndexView index) {
        // Only process if the parameter type is ENUM or MULTI_ENUM
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

        // Ensure we have a CLASS type
        if (enumType == null || enumType.kind() != Type.Kind.CLASS) {
            log.debugf("Type is not a CLASS, cannot extract enum values");
            return List.of();
        }

        // Get the class info from the index
        ClassInfo enumClassInfo = index.getClassByName(enumType.name());
        if (enumClassInfo == null) {
            log.debugf("Could not find class info for type: %s", enumType.name());
            return List.of();
        }

        // Check if it's actually an enum
        if (!enumClassInfo.isEnum()) {
            log.debugf("Class %s is not an enum", enumType.name());
            return List.of();
        }

        // Extract enum constant names
        List<String> enumValues = new ArrayList<>();
        for (FieldInfo field : enumClassInfo.fields()) {
            // Enum constants are static final fields of the enum type itself
            if (field.isEnumConstant()) {
                enumValues.add(field.name());
            }
        }

        log.debugf("Extracted %s enum values from %s: %s", enumValues.size(), enumType.name(), enumValues);
        return enumValues;
    }
}

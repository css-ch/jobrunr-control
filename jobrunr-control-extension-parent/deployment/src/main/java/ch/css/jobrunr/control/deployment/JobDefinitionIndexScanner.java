package ch.css.jobrunr.control.deployment;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.annotations.JobParameterDefinition;
import ch.css.jobrunr.control.domain.JobDefinition;
import ch.css.jobrunr.control.domain.JobParameter;
import ch.css.jobrunr.control.domain.JobParameterType;
import org.jboss.jandex.*;
import org.jobrunr.jobs.lambdas.JobRequest;
import org.jobrunr.jobs.lambdas.JobRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Scans the Jandex index for JobRequestHandler implementations annotated with @ConfigurableJob
 * and extracts job definitions.
 */
class JobDefinitionIndexScanner {

    private static final Logger log = LoggerFactory.getLogger(JobDefinitionIndexScanner.class);

    private static final DotName JOB_REQUEST_HANDLER = DotName.createSimple(JobRequestHandler.class.getName());
    private static final DotName JOB_REQUEST = DotName.createSimple(JobRequest.class.getName());

    private JobDefinitionIndexScanner() {
        // Helper
    }

    public static Set<JobDefinition> findJobSpecifications(IndexView index) {
        Set<JobDefinition> jobDefinitions = new java.util.HashSet<>();
        log.debug("Searching for JobRequestHandler implementations");
        // Find all classes that implement JobRequestHandler
        var implementations = index.getAllKnownImplementations(JOB_REQUEST_HANDLER);

        for (ClassInfo classInfo : implementations) {
            log.debug("Inspecting implementation: {}", classInfo.name());
            // Check if the class has a method annotated with @ConfigurableJob
            MethodInfo runMethod = findRunMethodWithConfigurableJobAnnotation(classInfo);
            if (runMethod != null) {
                log.debug("Found @ConfigurableJob run method on {}", classInfo.name());
                // Extract the type parameter from JobRequestHandler
                Type jobRequestType = findParameterizedInterfaceArgument(classInfo, index, JOB_REQUEST_HANDLER);
                if (jobRequestType != null && jobRequestType.kind() == Type.Kind.CLASS) {
                    log.debug("JobRequest type resolved: {}", jobRequestType.name());
                    ClassInfo requestClassInfo = index.getClassByName(jobRequestType.name());
                    if (requestClassInfo != null && isRecord(requestClassInfo) && implementsInterfaceRecursive(requestClassInfo, index, JOB_REQUEST)) {
                        // Extract job type and batch flag
                        String jobType = classInfo.simpleName();
                        boolean isBatchJob = getBatchJobFlag(runMethod);

                        // Analyze the record parameters
                        List<JobParameter> parameters = analyzeRecordParameters(requestClassInfo, index);

                        // Extract field names from parameters
                        log.info("Discovered job: {} (batch={}) with {} parameters", jobType, isBatchJob, parameters.size());

                        jobDefinitions.add(new JobDefinition(
                                jobType,
                                isBatchJob,
                                requestClassInfo.name().toString(),
                                classInfo.name().toString(), // handlerClassName
                                parameters,
                                isRecord(requestClassInfo)
                        ));
                    } else {
                        log.debug("Request class {} is not a valid record JobRequest or does not implement JobRequest", jobRequestType.name());
                    }
                } else {
                    log.debug("Could not resolve parameterized JobRequest type");
                }
            }
        }
        return jobDefinitions;
    }

    private static MethodInfo findRunMethodWithConfigurableJobAnnotation(ClassInfo classInfo) {
        for (MethodInfo method : classInfo.methods()) {
            log.debug("Search @ConfigurableJob run method on {}:{}", classInfo.name(), method.name());
            log.debug("   Annotations are {}", classInfo.annotations());
            log.debug("   Parameters are {}", method.name().equals("run"));

            if (method.name().equals("run") && method.hasAnnotation(ConfigurableJob.class)) {
                log.debug("Method {}#{} annotated with @ConfigurableJob", classInfo.name(), method.name());
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
            log.debug(">> Inspecting interface: {}", interfaceType.name());
            if (interfaceType.kind() == Type.Kind.PARAMETERIZED_TYPE) {
                ParameterizedType pt = interfaceType.asParameterizedType();
                if (pt.name().equals(targetInterface) && !pt.arguments().isEmpty()) {
                    log.debug("Found parameterized interface {} on {} -> argument {}", targetInterface, classInfo.name(), pt.arguments().getFirst().name());
                    return pt.arguments().getFirst();
                }

                // Check if this parameterized interface extends/implements the target interface
                // e.g., ConfigurableJob<T> extends JobRequestHandler<T>
                ClassInfo ifaceInfo = index.getClassByName(pt.name());
                if (ifaceInfo != null && implementsInterfaceRecursive(ifaceInfo, index, targetInterface)) {
                    // This interface extends the target, so return its type argument
                    if (!pt.arguments().isEmpty()) {
                        log.debug("Found parameterized interface {} (which extends {}) on {} -> argument {}",
                                pt.name(), targetInterface, classInfo.name(), pt.arguments().getFirst().name());
                        return pt.arguments().getFirst();
                    }
                }
            } else if (interfaceType.name().equals(targetInterface)) {
                // raw type found, no type argument
                log.debug("Found raw interface {} on {}", targetInterface, classInfo.name());
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
                Type found = findParameterizedInterfaceArgument(superClass, index, targetInterface);
                if (found != null) {
                    return found;
                }
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
            if (superClass != null && implementsInterfaceRecursive(superClass, index, targetInterface)) {
                return true;
            }
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

    private static List<JobParameter> analyzeRecordParameters(ClassInfo recordClass, IndexView index) {
        List<JobParameter> parameters = new ArrayList<>();

        // For records, the fields represent the record components
        for (FieldInfo field : recordClass.fields()) {
            JobParameter parameter = analyzeField(field, index);
            parameters.add(parameter);
        }

        return parameters;
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
            log.debug("Type is null or has no name; defaulting to STRING");
            return JobParameterType.STRING;
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

        log.debug("Mapped type '{}' to JobParameterType.{}", typeName, resolved.name());
        return resolved;
    }

    private static List<String> getEnumValuesIfApplicable(Type type, JobParameterType parameterType, IndexView index) {
        // Only process if the parameter type is ENUM
        if (parameterType != JobParameterType.ENUM) {
            return List.of();
        }

        // Ensure we have a CLASS type
        if (type == null || type.kind() != Type.Kind.CLASS) {
            log.debug("Type is not a CLASS, cannot extract enum values");
            return List.of();
        }

        // Get the class info from the index
        ClassInfo enumClassInfo = index.getClassByName(type.name());
        if (enumClassInfo == null) {
            log.debug("Could not find class info for type: {}", type.name());
            return List.of();
        }

        // Check if it's actually an enum
        if (!enumClassInfo.isEnum()) {
            log.debug("Class {} is not an enum", type.name());
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

        log.debug("Extracted {} enum values from {}: {}", enumValues.size(), type.name(), enumValues);
        return enumValues;
    }
}

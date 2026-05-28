package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.domain.JobRecapParameter;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Extracts recap parameters from record components annotated with @JobRecapParameter.
 * Similar to ParameterExtractor, but specifically for recap metadata extraction.
 */
public class RecapParameterExtractor {

    private static final Logger LOG = Logger.getLogger(RecapParameterExtractor.class);

    private static final DotName JOB_RECAP_PARAMETER = DotName.createSimple(ch.css.jobrunr.control.annotations.JobRecapParameter.class.getName());

    private final IndexView index;

    public RecapParameterExtractor(IndexView index) {
        this.index = index;
    }

    /**
     * Analyzes a recap record class and extracts @JobRecapParameter annotations from its record components.
     * 
     * @param recapParameterClass fully qualified class name (e.g., "ch.css.jobrunr.control.jobs.complex.ComplexParameterDemoJobRecap")
     * @return List of JobRecapParameter domain objects extracted from the class
     */
    public List<JobRecapParameter> analyzeRecapParameters(String recapParameterClass) {
        DotName recapClassName = DotName.createSimple(recapParameterClass);
        ClassInfo recapClazz = index.getClassByName(recapClassName);
        
        if (recapClazz == null) {
            LOG.warnf("Recap class not found in Jandex index: %s", recapParameterClass);
            return List.of();
        }

        if (!recapClazz.isRecord()) {
            LOG.warnf("Recap class %s is not a record, cannot extract record components", recapParameterClass);
            return List.of();
        }

        List<JobRecapParameter> recapParameters = new ArrayList<>();
        List<RecordComponentInfo> recordComponents = recapClazz.recordComponents();

        for (RecordComponentInfo component : recordComponents) {
            JobRecapParameter recapParam = analyzeRecordComponent(component);
            if (recapParam != null) {
                recapParameters.add(recapParam);
            }
        }

        // Sort by order
        recapParameters.sort(Comparator.comparingInt(JobRecapParameter::order));

        LOG.debugf("Extracted %d recap parameters from %s", recapParameters.size(), recapParameterClass);
        return recapParameters;
    }

    /**
     * Analyzes a single record component for @JobRecapParameter annotation.
     * Only processes components with type int, long, Integer, or Long.
     *
     * @param component the record component to analyze
     * @return JobRecapParameter if @JobRecapParameter annotation is present and type is valid, otherwise null
     */
    private JobRecapParameter analyzeRecordComponent(RecordComponentInfo component) {
        AnnotationInstance recapParamAnnotation = component.annotation(JOB_RECAP_PARAMETER);
        
        if (recapParamAnnotation == null) {
            LOG.debugf("Record component '%s' has no @JobRecapParameter annotation, skipping", component.name());
            return null;
        }

        // Validate component type: must be int, long, Integer, or Long
        Type componentType = component.type();
        if (!isValidRecapParameterType(componentType)) {
            LOG.warnf("Record component '%s' has unsupported type '%s'. Only int, long, Integer, or Long are supported. Skipping.",
                    component.name(), componentType.name());
            return null;
        }

        String name = component.name();
        String displayName = "";
        String description = "";
        String icon = "";
        String css = "";
        String section = "";
        int order = 999;

        // Extract displayName
        AnnotationValue displayNameValue = recapParamAnnotation.value("displayName");
        if (displayNameValue != null && !displayNameValue.asString().isEmpty()) {
            displayName = displayNameValue.asString();
        }

        // Extract description
        AnnotationValue descriptionValue = recapParamAnnotation.value("description");
        if (descriptionValue != null && !descriptionValue.asString().isEmpty()) {
            description = descriptionValue.asString();
        }

        // Extract icon
        AnnotationValue iconValue = recapParamAnnotation.value("icon");
        if (iconValue != null && !iconValue.asString().isEmpty()) {
            icon = iconValue.asString();
        }

        // Extract css
        AnnotationValue cssValue = recapParamAnnotation.value("css");
        if (cssValue != null && !cssValue.asString().isEmpty()) {
            css = cssValue.asString();
        }

        // Extract section
        AnnotationValue sectionValue = recapParamAnnotation.value("section");
        if (sectionValue != null && !sectionValue.asString().isEmpty()) {
            section = sectionValue.asString();
        }

        // Extract order
        AnnotationValue orderValue = recapParamAnnotation.value("order");
        if (orderValue != null) {
            order = orderValue.asInt();
        }

        LOG.debugf("Extracted recap parameter '%s' with displayName='%s', section='%s', order=%d from component", name, displayName, section, order);
        return new JobRecapParameter(name, displayName, description, icon, css, section, order);
    }

    /**
     * Validates if the given type is a valid recap parameter type.
     * Only int, long, Integer, or Long are supported.
     *
     * @param type the Jandex type to validate
     * @return true if type is int, long, Integer, or Long; false otherwise
     */
    private boolean isValidRecapParameterType(Type type) {
        if (type == null || type.name() == null) {
            return false;
        }

        String typeName = type.name().toString();

        // Check for primitive types
        if (type.kind() == Type.Kind.PRIMITIVE) {
            return "int".equals(typeName) || "long".equals(typeName) ||
                   "I".equals(typeName) || "J".equals(typeName);
        }

        // Check for boxed types
        return "java.lang.Integer".equals(typeName) || "java.lang.Long".equals(typeName) ||
               "Integer".equals(typeName) || "Long".equals(typeName);
    }
}

package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.annotations.ConfigurableJob;
import ch.css.jobrunr.control.domain.JobSettings;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts job settings from @ConfigurableJob annotations.
 * Handles all configuration attributes like name, retries, labels, filters, etc.
 */
public class JobSettingsExtractor {

    /**
     * Finds the run method annotated with @ConfigurableJob.
     *
     * @param classInfo the class containing the method
     * @return the annotated method, or null if not found
     */
    public MethodInfo findConfigurableJobMethod(org.jboss.jandex.ClassInfo classInfo) {
        for (MethodInfo method : classInfo.methods()) {
            if (method.name().equals("run") && method.hasAnnotation(ConfigurableJob.class)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Extracts JobSettings from a @ConfigurableJob annotation.
     *
     * @param method the method annotated with @ConfigurableJob
     * @return the extracted job settings
     */
    public JobSettings extractJobSettings(MethodInfo method) {
        AnnotationInstance annotation = method.annotation(ConfigurableJob.class);
        if (annotation == null) {
            return createDefaultJobSettings();
        }

        String name = getAnnotationValue(annotation, "name", "");
        boolean isBatch = getAnnotationValue(annotation, "isBatch", false);
        int retries = getAnnotationValue(annotation, "retries", ConfigurableJob.NBR_OF_RETRIES_NOT_PROVIDED);
        List<String> labels = getAnnotationValueAsStringList(annotation, "labels");
        List<String> jobFilters = getAnnotationValueAsClassNameList(annotation, "jobFilters");
        String queue = getAnnotationValue(annotation, "queue", "");
        String runOnServerWithTag = getAnnotationValue(annotation, "runOnServerWithTag", "");
        String mutex = getAnnotationValue(annotation, "mutex", "");
        String rateLimiter = getAnnotationValue(annotation, "rateLimiter", "");
        String processTimeOut = getAnnotationValue(annotation, "processTimeOut", "");
        String deleteOnSuccess = getAnnotationValue(annotation, "deleteOnSuccess", "");
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

    /**
     * Gets the batch job flag from a method's @ConfigurableJob annotation.
     *
     * @param method the method to check
     * @return true if the job is a batch job
     */
    public boolean getBatchJobFlag(MethodInfo method) {
        AnnotationInstance annotation = method.annotation(ConfigurableJob.class);
        if (annotation != null) {
            AnnotationValue value = annotation.value("isBatch");
            return value != null && value.asBoolean();
        }
        return false;
    }

    private JobSettings createDefaultJobSettings() {
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

    private String getAnnotationValue(AnnotationInstance annotation, String name, String defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asString() : defaultValue;
    }

    private boolean getAnnotationValue(AnnotationInstance annotation, String name, boolean defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asBoolean() : defaultValue;
    }

    private int getAnnotationValue(AnnotationInstance annotation, String name, int defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asInt() : defaultValue;
    }

    private List<String> getAnnotationValueAsStringList(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        if (value == null) {
            return List.of();
        }
        return List.of(value.asStringArray());
    }

    private List<String> getAnnotationValueAsClassNameList(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        if (value == null) {
            return List.of();
        }
        Type[] types = value.asClassArray();
        List<String> result = new ArrayList<>();
        for (Type type : types) {
            result.add(type.name().toString());
        }
        return result;
    }
}

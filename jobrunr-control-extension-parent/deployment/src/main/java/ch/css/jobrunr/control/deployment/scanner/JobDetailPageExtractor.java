package ch.css.jobrunr.control.deployment.scanner;

import ch.css.jobrunr.control.domain.JobDetailPage;
import org.jboss.jandex.*;
import org.jboss.logging.Logger;

public class JobDetailPageExtractor {

    private static final Logger LOG = Logger.getLogger(JobDetailPageExtractor.class);

    public JobDetailPage extractJobDetailPage(MethodInfo method) {
        AnnotationInstance annotation = method.annotation(ch.css.jobrunr.control.annotations.JobDetailPage.class);
        if (annotation == null) {
            return null;
        }
        String recapParameterClass = getAnnotationValueAsString(annotation, "recapParameterClass");
        String messageProviderKey = getAnnotationValueAsString(annotation, "messageProviderKey", "");
        String recapProviderKey = getAnnotationValueAsString(annotation, "recapProviderKey", "");
        boolean showRecapParameterWithZeroValue = getAnnotationValue(annotation, "showRecapParameterWithZeroValue", true);
        boolean showEmptyParameters = getAnnotationValue(annotation, "showEmptyParameters", true);

        return new JobDetailPage(recapParameterClass, messageProviderKey, recapProviderKey, showRecapParameterWithZeroValue, showEmptyParameters);
    }

    private String getAnnotationValueAsString(AnnotationInstance annotation, String name) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asClass().name().toString() : null;
    }

    private String getAnnotationValueAsString(AnnotationInstance annotation, String name, String defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asString() : defaultValue;
    }

    private boolean getAnnotationValue(AnnotationInstance annotation, String name, boolean defaultValue) {
        AnnotationValue value = annotation.value(name);
        return value != null ? value.asBoolean() : defaultValue;
    }
}

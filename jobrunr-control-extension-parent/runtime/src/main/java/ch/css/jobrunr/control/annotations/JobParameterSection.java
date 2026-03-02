package ch.css.jobrunr.control.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define metadata for job parameters (record components or fields).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
public @interface JobParameterSection {

    /**
     * Unique identifier for the parameter section. Used for grouping parameters in the UI.
     */
    String id();

    /**
     * Display title for the parameter section in the UI.
     */
    String title();

    /**
     * Layout of the parameter section in the UI.
     */
    JobParameterSectionLayout layout() default JobParameterSectionLayout.SINGLE_VALUE_ON_LINE_LABEL_OBOVE;

    /**
     * Order of the parameter section in the UI. Lower values are displayed first.
     */
    int order() default 9999;
}

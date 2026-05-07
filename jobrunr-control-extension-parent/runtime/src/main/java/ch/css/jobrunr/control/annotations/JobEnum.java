package ch.css.jobrunr.control.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JobEnum {
    /**
     * The label ist shown in the select-list instant of the enum's-name.
     */
    String label();

    /**
     * Order of the enum in the select-list. Lower values are displayed first.
     */
    int order() default 9999;
}


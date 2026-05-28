package ch.css.jobrunr.control.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.RECORD_COMPONENT, ElementType.FIELD, ElementType.PARAMETER})
public @interface JobRecapParameter {

    /**
     * This will be printed at the Recap-Counter on the Details-Page
     */
    String displayName() default "";

    /**
     * The description is shown when hovering over the Recap-Counter on the Details-Page. It can be used to provide more detailed information about the parameter.
     */
    String description() default "";

    /**
     * The CSS-Icon will be shown before the displayName in the Recap-Counter on the Details-Page. You can use any CSS-Icon from libraries like FontAwesome or Bootstrap Icons, or even custom icons defined in your application's CSS.
     * Look at the Bootstrap Icons documentation for examples: https://icons.getbootstrap.com/
     */
    String icon() default "";

    /**
     * The css property allows you to add custom CSS styles to the Recap-Counter on the Details-Page. This can be used to change the appearance of the counter, such as its color, font size, or background.
     * Example: "color: red; font-size: 14px;"
     */
    String css() default "";

    /**
     * The css section Property will group the Recap-Parameters with the identical section and will show a Sum under the grouped Recap-Counters.
     */
    String section() default "";

    /**
     * The order attribute determines the position of the parameter in the Recap-Counter on the Details-Page. Parameters with lower order values will be displayed before those with higher values. This allows you to control the sequence in which parameters are shown.
     */
    int order() default 999;
}

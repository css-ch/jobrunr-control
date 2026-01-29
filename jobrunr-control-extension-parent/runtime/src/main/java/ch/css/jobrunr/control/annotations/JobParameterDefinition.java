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
public @interface JobParameterDefinition {

    /**
     * Constant for no default value.
     */
    String NO_DEFAULT_VALUE = "__NO_DEFAULT_VALUE__";

    /**
     * The name of the parameter. If empty, the field name is used.
     */
    String name() default "";

    /**
     * The default value of the parameter as a String.
     * It will be parsed to the target type at runtime.
     * Use standard formats:
     * - String: "some text"
     * - boolean: "true" or "false"
     * - Integer: "123"
     * - Enum: "ENUM_VALUE"
     * - LocalDate: "yyyy-MM-dd" (e.g., "2023-12-31")
     * - LocalDateTime: ISO-8601 (e.g., "2023-12-31T23:59:59")
     */
    String defaultValue() default NO_DEFAULT_VALUE;

    /**
     * Optional type specification for external parameters.
     * When used within {@literal @}JobParameterSet, this defines the parameter type.
     * Format: fully qualified class name (e.g., "java.lang.String", "java.time.LocalDate")
     * For EnumSet: "java.util.EnumSet&lt;com.example.MyEnum&gt;"
     * If not specified, type is inferred from record component (inline parameters only).
     */
    String type() default "";
}

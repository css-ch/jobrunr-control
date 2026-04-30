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
     * The display name of the parameter for UI purposes. If empty, the field name is used.
     */
    String displayName() default "";

    /**
     * A brief description of the parameter for UI tooltips or documentation.
     */
    String description() default "";

    /**
     * The ID of the ParameterSection this parameter belongs to. Used for grouping parameters in the UI.
     */
    String sectionId() default "default";

    /**
     * The order of the parameter within its section. Lower values are displayed first.
     */
    int order() default 9999;

    /**
     * Whether the parameter must be provided by the user.
     * Defaults to {@code true}. Set to {@code false} to declare an optional parameter.
     * A {@link #defaultValue()} does not influence this flag; the two properties are independent.
     */
    boolean required() default true;

    /**
     * The default value of the parameter as a String.
     * It will be parsed to the target type at runtime.
     * Use standard formats:
     * - String: "some text"
     * - MULTILINE: "line 1\nline 2\nline 3"
     * - Integer/Long: "123"
     * - Double/Float: "3.14159"
     * - Boolean: "true" or "false"
     * - Enum: "ENUM_VALUE"
     * - EnumSet: "VALUE1,VALUE2,VALUE3"
     * - LocalDate: "yyyy-MM-dd" (e.g., "2024-12-31")
     * - LocalDateTime: ISO-8601 (e.g., "2024-12-31T23:59:59")
     */
    String defaultValue() default NO_DEFAULT_VALUE;

    /**
     * Optional type specification. Accepts either a fully qualified class name or the
     * special alias {@code "MULTILINE"} to render a String field as a multi-line textarea.
     * <p>
     * Supported values:
     * <ul>
     *   <li>{@code "MULTILINE"} — renders a {@code String} field as a multi-line textarea in the UI</li>
     *   <li>{@code "java.lang.String"}</li>
     *   <li>{@code "java.lang.Integer"} / {@code "int"} / {@code "java.lang.Long"} / {@code "long"}</li>
     *   <li>{@code "java.lang.Double"} / {@code "double"} / {@code "java.lang.Float"} / {@code "float"}</li>
     *   <li>{@code "java.lang.Boolean"} / {@code "boolean"}</li>
     *   <li>{@code "java.time.LocalDate"}</li>
     *   <li>{@code "java.time.LocalDateTime"}</li>
     *   <li>Fully qualified enum class name (e.g., {@code "com.example.MyEnum"})</li>
     *   <li>{@code "java.util.EnumSet<com.example.MyEnum>"} — multi-select</li>
     * </ul>
     * If not specified, the type is inferred from the record component (inline parameters only).
     */
    String type() default "";
}

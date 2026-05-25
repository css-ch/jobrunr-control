package ch.css.jobrunr.control.domain;

/**
 * Represents a job parameter.
 * Contains metadata such as name, type, required status, and default value.
 *
 * @param name         Parameter name
 * @param displayName  This will be printed at the Recap-Counter on the Details-Page
 * @param description  The description is shown when hovering over the Recap-Counter on the Details-Page. It can be used to provide more detailed information about the parameter.
 * @param icon         The CSS-Icon will be shown before the displayName in the Recap-Counter on the Details-Page. You can use any CSS-Icon from libraries like FontAwesome or Bootstrap Icons, or even custom icons defined in your application's CSS.
 * @param css          The css property allows you to add custom CSS styles to the Recap-Counter on the Details-Page. This can be used to change the appearance of the counter, such as its color, font size, or background.
 * @param order        Declaration order (0-based index)
 */
public record JobRecapParameter(
        String name,
        String displayName,
        String description,
        String icon,
        String css,
        int order) {
}

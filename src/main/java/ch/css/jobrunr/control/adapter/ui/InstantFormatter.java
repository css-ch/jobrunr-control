package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.qute.TemplateExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Provides formatting for {@link Instant} objects in Qute templates.
 * <p>
 * Example usage in a Qute template:
 * <pre>
 *   &lt;span class="text-muted"&gt;{job.scheduledAt.format('dd.MM.yyyy HH:mm')}&lt;/span&gt;
 * </pre>
 */
@TemplateExtension
public class InstantFormatter {

    /**
     * Allows formatting of {@link Instant} objects in Qute templates using a custom pattern.
     * Automatically uses the system time zone of the server.
     *
     * @param instant the instant to format
     * @param pattern the date-time pattern to use
     * @return the formatted date-time string, or an empty string if the instant is null
     */
    public static String format(Instant instant, String pattern) {
        if (instant == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern(pattern)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }
}
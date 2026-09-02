package ch.css.jobrunr.control.adapter.ui;

import io.quarkus.qute.TemplateExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Provides formatting for job parameter values in Qute templates.
 * <p>
 * Formats values according to Swiss conventions:
 * - LocalDate: dd.MM.yyyy
 * - LocalDateTime: dd.MM.yyyy HH:mm, with seconds and/or milliseconds appended when present
 * - Instant: dd.MM.yyyy HH:mm (converted to Swiss timezone)
 * - Boolean: Ja/Nein
 * - Others: String representation
 * <p>
 * Example usage in a Qute template:
 * <pre>
 *   {param.value.formatParameterValue().raw}
 * </pre>
 */
@TemplateExtension(namespace = "format")
public class ParameterValueFormatter {

    private static final DateTimeFormatter SWISS_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SWISS_DATETIME_MINUTES_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter SWISS_DATETIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DateTimeFormatter SWISS_DATETIME_MILLIS_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS");
    private static final ZoneId SWISS_ZONE = ZoneId.of("Europe/Zurich");

    /**
     * Formats a parameter value based on its type using Swiss formatting conventions.
     *
     * @param value The parameter value to format
     * @return Formatted string representation
     */
    @SuppressWarnings("unused") // Used by type-safe Qute templates
    public static String formatParameterValue(Object value) {
        if (value == null) {
            return "<i class=\"text-muted\">null</i>";
        }

        if (value instanceof LocalDate localDate) {
            return localDate.format(SWISS_DATE_FORMATTER);
        }

        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(dateTimeFormatterFor(localDateTime));
        }

        if (value instanceof String str) {
            try {
                LocalDateTime localDateTime = LocalDateTime.parse(str);
                return localDateTime.format(dateTimeFormatterFor(localDateTime));
            } catch (Exception ignored) {
            }
            try {
                return LocalDate.parse(str).format(SWISS_DATE_FORMATTER);
            } catch (Exception ignored) {
            }
        }

        if (value instanceof Instant instant) {
            return instant.atZone(SWISS_ZONE).format(SWISS_DATETIME_MINUTES_FORMATTER);
        }

        if (value instanceof Boolean bool) {
            return bool ? "Ja" : "Nein";
        }

        return value.toString();
    }

    /**
     * Picks the display formatter based on the actual precision carried by the value,
     * so that seconds/milliseconds are only shown when they were actually set.
     */
    private static DateTimeFormatter dateTimeFormatterFor(LocalDateTime localDateTime) {
        if (localDateTime.getNano() != 0) {
            return SWISS_DATETIME_MILLIS_FORMATTER;
        }
        if (localDateTime.getSecond() != 0) {
            return SWISS_DATETIME_SECONDS_FORMATTER;
        }
        return SWISS_DATETIME_MINUTES_FORMATTER;
    }
}

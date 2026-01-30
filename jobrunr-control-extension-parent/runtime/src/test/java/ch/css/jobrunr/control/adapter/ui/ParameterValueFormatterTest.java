package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParameterValueFormatterTest {

    @Test
    void shouldFormatLocalDateWithSwissFormat() {
        LocalDate date = LocalDate.of(2024, 3, 15);

        String result = ParameterValueFormatter.formatParameterValue(date);

        assertEquals("15.03.2024", result);
    }

    @Test
    void shouldFormatLocalDateTimeWithSwissFormat() {
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30);

        String result = ParameterValueFormatter.formatParameterValue(dateTime);

        assertEquals("15.03.2024 14:30", result);
    }

    @Test
    void shouldFormatInstantWithSwissTimezone() {
        Instant instant = LocalDateTime.of(2024, 3, 15, 14, 30)
                .atZone(ZoneId.of("Europe/Zurich"))
                .toInstant();

        String result = ParameterValueFormatter.formatParameterValue(instant);

        assertEquals("15.03.2024 14:30", result);
    }

    @Test
    void shouldFormatBooleanTrueAsJa() {
        String result = ParameterValueFormatter.formatParameterValue(true);

        assertEquals("Ja", result);
    }

    @Test
    void shouldFormatBooleanFalseAsNein() {
        String result = ParameterValueFormatter.formatParameterValue(false);

        assertEquals("Nein", result);
    }

    @Test
    void shouldFormatNullAsItalicNull() {
        String result = ParameterValueFormatter.formatParameterValue(null);

        assertEquals("<i class=\"text-muted\">null</i>", result);
    }

    @Test
    void shouldFormatStringAsIs() {
        String result = ParameterValueFormatter.formatParameterValue("Simple text");

        assertEquals("Simple text", result);
    }

    @Test
    void shouldFormatLocalDateTimeString() {
        String result = ParameterValueFormatter.formatParameterValue("2024-03-15T14:30:00");

        assertEquals("15.03.2024 14:30", result);
    }

    @Test
    void shouldFormatLocalDateString() {
        String result = ParameterValueFormatter.formatParameterValue("2024-03-15");

        assertEquals("15.03.2024", result);
    }

    @Test
    void shouldFormatNumbersAsString() {
        String result = ParameterValueFormatter.formatParameterValue(12345);

        assertEquals("12345", result);
    }
}

package ch.css.jobrunr.control.adapter.ui;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstantFormatterTest {

    @Test
    void shouldFormatInstantWithGivenPattern() {
        Instant instant = ZonedDateTime.of(2024, 3, 15, 14, 30, 45, 0, ZoneId.systemDefault()).toInstant();

        String result = InstantFormatter.format(instant, "dd.MM.yyyy HH:mm");

        assertEquals("15.03.2024 14:30", result);
    }

    @Test
    void shouldFormatInstantWithDifferentPattern() {
        Instant instant = ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 0, ZoneId.systemDefault()).toInstant();

        String result = InstantFormatter.format(instant, "yyyy-MM-dd HH:mm:ss");

        assertEquals("2024-12-31 23:59:59", result);
    }

    @Test
    void shouldReturnEmptyStringWhenInstantIsNull() {
        String result = InstantFormatter.format(null, "dd.MM.yyyy HH:mm");

        assertEquals("", result);
    }
}

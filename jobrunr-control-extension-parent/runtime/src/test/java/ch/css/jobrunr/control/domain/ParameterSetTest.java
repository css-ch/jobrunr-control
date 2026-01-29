package ch.css.jobrunr.control.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ParameterSetTest {

    @Test
    void shouldCreateParameterSetWithValidData() {
        UUID id = UUID.randomUUID();
        String jobType = "TestJob";
        Map<String, Object> params = Map.of("key", "value");

        ParameterSet paramSet = ParameterSet.create(id, jobType, params);

        assertNotNull(paramSet);
        assertEquals(id, paramSet.id());
        assertEquals(jobType, paramSet.jobType());
        assertEquals(params, paramSet.parameters());
        assertNotNull(paramSet.createdAt());
        assertNotNull(paramSet.lastAccessedAt());
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new ParameterSet(null, "TestJob", Map.of(), Instant.now(), Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenJobTypeIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new ParameterSet(UUID.randomUUID(), null, Map.of(), Instant.now(), Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenJobTypeIsBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                new ParameterSet(UUID.randomUUID(), "", Map.of(), Instant.now(), Instant.now())
        );
    }

    @Test
    void shouldThrowExceptionWhenParametersIsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                new ParameterSet(UUID.randomUUID(), "TestJob", null, Instant.now(), Instant.now())
        );
    }

    @Test
    void shouldMarkAccessed() throws InterruptedException {
        ParameterSet original = ParameterSet.create(UUID.randomUUID(), "TestJob", Map.of());
        Instant originalAccessTime = original.lastAccessedAt();

        Thread.sleep(10); // Ensure time difference

        ParameterSet updated = original.markAccessed();

        assertEquals(original.id(), updated.id());
        assertEquals(original.jobType(), updated.jobType());
        assertEquals(original.parameters(), updated.parameters());
        assertEquals(original.createdAt(), updated.createdAt());
        assertTrue(updated.lastAccessedAt().isAfter(originalAccessTime));
    }
}

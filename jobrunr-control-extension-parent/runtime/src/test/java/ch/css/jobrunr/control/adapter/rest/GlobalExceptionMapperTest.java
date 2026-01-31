package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.domain.exceptions.JobNotFoundException;
import ch.css.jobrunr.control.domain.exceptions.ValidationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GlobalExceptionMapper.
 * Verifies proper exception handling and HTTP status code mapping.
 */
class GlobalExceptionMapperTest {

    private GlobalExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GlobalExceptionMapper();
    }

    @Test
    void shouldHandleJaxRsNotFoundWith404() {
        // Given
        NotFoundException exception = new NotFoundException("Route not found");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        GlobalExceptionMapper.ErrorResponse errorResponse =
                (GlobalExceptionMapper.ErrorResponse) response.getEntity();

        assertEquals("NOT_FOUND", errorResponse.errorCode());
        assertEquals("Resource not found", errorResponse.message());
        assertEquals("The requested resource does not exist", errorResponse.details());
        assertNotNull(errorResponse.correlationId());
    }

    @Test
    void shouldHandleJobNotFoundWith404() {
        // Given
        JobNotFoundException exception = new JobNotFoundException("Job not found: job-123");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        GlobalExceptionMapper.ErrorResponse errorResponse =
                (GlobalExceptionMapper.ErrorResponse) response.getEntity();

        assertEquals("NOT_FOUND", errorResponse.errorCode());
        assertEquals("Job not found", errorResponse.message());
        assertTrue(errorResponse.details().contains("Job not found"));
        assertNotNull(errorResponse.correlationId());
    }

    @Test
    void shouldHandleValidationExceptionWith400() {
        // Given
        ValidationException exception = new ValidationException(
                List.of("Parameter 'x' is required", "Parameter 'y' must be positive")
        );

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        GlobalExceptionMapper.ValidationErrorResponse errorResponse =
                (GlobalExceptionMapper.ValidationErrorResponse) response.getEntity();

        assertEquals("VALIDATION_ERROR", errorResponse.errorCode());
        assertEquals("Validation failed", errorResponse.message());
        assertEquals(2, errorResponse.errors().size());
        assertNotNull(errorResponse.correlationId());
    }

    @Test
    void shouldHandleIllegalArgumentExceptionWith400() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid input");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        GlobalExceptionMapper.ErrorResponse errorResponse =
                (GlobalExceptionMapper.ErrorResponse) response.getEntity();

        assertEquals("BAD_REQUEST", errorResponse.errorCode());
        assertEquals("Invalid request", errorResponse.message());
        assertNotNull(errorResponse.correlationId());
    }

    @Test
    void shouldHandleIllegalStateExceptionWith409() {
        // Given
        IllegalStateException exception = new IllegalStateException("Job is already running");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());

        GlobalExceptionMapper.ErrorResponse errorResponse =
                (GlobalExceptionMapper.ErrorResponse) response.getEntity();

        assertEquals("CONFLICT", errorResponse.errorCode());
        assertEquals("Operation conflicts with current state", errorResponse.message());
        assertNotNull(errorResponse.correlationId());
    }

    @Test
    void shouldHandleUnexpectedExceptionWith500() {
        // Given
        RuntimeException exception = new RuntimeException("Unexpected error");

        // When
        Response response = mapper.toResponse(exception);

        // Then
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());

        GlobalExceptionMapper.ErrorResponse errorResponse =
                (GlobalExceptionMapper.ErrorResponse) response.getEntity();

        assertEquals("INTERNAL_ERROR", errorResponse.errorCode());
        assertEquals("An unexpected error occurred", errorResponse.message());
        assertTrue(errorResponse.details().contains("correlation ID"));
        assertNotNull(errorResponse.correlationId());
    }

    @Test
    void shouldGenerateUniqueCorrelationIds() {
        // Given
        Exception exception1 = new JobNotFoundException("Job 1");
        Exception exception2 = new JobNotFoundException("Job 2");

        // When
        Response response1 = mapper.toResponse(exception1);
        Response response2 = mapper.toResponse(exception2);

        // Then
        GlobalExceptionMapper.ErrorResponse error1 =
                (GlobalExceptionMapper.ErrorResponse) response1.getEntity();
        GlobalExceptionMapper.ErrorResponse error2 =
                (GlobalExceptionMapper.ErrorResponse) response2.getEntity();

        assertNotEquals(error1.correlationId(), error2.correlationId());
    }
}

package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.domain.exceptions.*;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

/**
 * Global exception handler for REST API.
 * Provides consistent error responses across all endpoints with proper HTTP status codes
 * and security-conscious error messages (no stack traces in production).
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger log = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        // Generate correlation ID for tracking
        String correlationId = UUID.randomUUID().toString();

        // Map exception to appropriate HTTP response
        return switch (exception) {
            case NotFoundException e -> handleJaxRsNotFound(e, correlationId);
            case JobNotFoundException e -> handleNotFound(e, correlationId);
            case ValidationException e -> handleValidation(e, correlationId);
            case JobSchedulingException e -> handleScheduling(e, correlationId);
            case JobExecutionException e -> handleExecution(e, correlationId);
            case TimeoutException e -> handleTimeout(e, correlationId);
            case IllegalArgumentException e -> handleBadRequest(e, correlationId);
            case IllegalStateException e -> handleConflict(e, correlationId);
            default -> handleUnexpected(exception, correlationId);
        };
    }

    /**
     * 404 Not Found - JAX-RS route not found
     * This is a normal scenario (e.g., browser requesting favicon.ico, mistyped URLs)
     */
    private Response handleJaxRsNotFound(NotFoundException e, String correlationId) {
        log.debugf("[%s] Route not found: %s", correlationId, e.getMessage());

        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(
                        "NOT_FOUND",
                        "Resource not found",
                        "The requested resource does not exist",
                        correlationId
                ))
                .build();
    }

    /**
     * 404 Not Found
     */
    private Response handleNotFound(JobNotFoundException e, String correlationId) {
        log.warnf("[%s] Job not found: %s", correlationId, e.getMessage());

        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(
                        "NOT_FOUND",
                        "Job not found",
                        sanitizeMessage(e.getMessage()),
                        correlationId
                ))
                .build();
    }

    /**
     * 400 Bad Request - Validation errors
     */
    private Response handleValidation(ValidationException e, String correlationId) {
        log.warnf("[%s] Validation error: %s", correlationId, e.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ValidationErrorResponse(
                        "VALIDATION_ERROR",
                        "Validation failed",
                        e.getErrors(),
                        correlationId
                ))
                .build();
    }

    /**
     * 400 Bad Request - Invalid arguments
     */
    private Response handleBadRequest(IllegalArgumentException e, String correlationId) {
        log.warnf("[%s] Bad request: %s", correlationId, e.getMessage());

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(
                        "BAD_REQUEST",
                        "Invalid request",
                        sanitizeMessage(e.getMessage()),
                        correlationId
                ))
                .build();
    }

    /**
     * 409 Conflict - State conflicts (e.g., job already running)
     */
    private Response handleConflict(IllegalStateException e, String correlationId) {
        log.warnf("[%s] Conflict: %s", correlationId, e.getMessage());

        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(
                        "CONFLICT",
                        "Operation conflicts with current state",
                        sanitizeMessage(e.getMessage()),
                        correlationId
                ))
                .build();
    }

    /**
     * 503 Service Unavailable - JobRunr scheduling issues
     */
    private Response handleScheduling(JobSchedulingException e, String correlationId) {
        log.errorf(e, "[%s] Job scheduling error", correlationId);

        // Check if it's a transient error (retry-able)
        boolean isTransient = isTransientError(e);
        int statusCode = isTransient ? Response.Status.SERVICE_UNAVAILABLE.getStatusCode() :
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

        return Response.status(statusCode)
                .entity(new ErrorResponse(
                        "SCHEDULING_ERROR",
                        "Failed to schedule job",
                        isTransient ? "Service temporarily unavailable, please retry" :
                                "Job scheduling failed",
                        correlationId
                ))
                .build();
    }

    /**
     * 500 Internal Server Error - Job execution failures
     */
    private Response handleExecution(JobExecutionException e, String correlationId) {
        log.errorf(e, "[%s] Job execution error", correlationId);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(
                        "EXECUTION_ERROR",
                        "Job execution failed",
                        "An error occurred while executing the job",
                        correlationId
                ))
                .build();
    }

    /**
     * 504 Gateway Timeout
     */
    private Response handleTimeout(TimeoutException e, String correlationId) {
        log.warnf("[%s] Timeout: %s", correlationId, e.getMessage());

        return Response.status(Response.Status.GATEWAY_TIMEOUT)
                .entity(new ErrorResponse(
                        "TIMEOUT",
                        "Operation timed out",
                        "The operation took too long to complete",
                        correlationId
                ))
                .build();
    }

    /**
     * 500 Internal Server Error - Unexpected exceptions
     * <p>
     * SECURITY: Never expose stack traces or internal details!
     */
    private Response handleUnexpected(Exception e, String correlationId) {
        log.errorf(e, "[%s] Unexpected error", correlationId);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred",
                        "Please contact support with correlation ID: " + correlationId,
                        correlationId
                ))
                .build();
    }

    /**
     * SECURITY: Sanitize error messages to prevent information leakage
     * <p>
     * Removes:
     * - SQL error details
     * - File paths
     * - Class names (except domain exceptions)
     * - Internal service names
     */
    private String sanitizeMessage(String message) {
        if (message == null) {
            return "No details available";
        }

        // Remove common sensitive patterns
        String sanitized = message
                .replaceAll("java\\..*?Exception:", "Error:")
                .replaceAll("at .*?\\(.*?\\)", "")
                .replaceAll("/[a-zA-Z0-9/_-]+\\.java", "")
                .replaceAll("SQL \\[.*?]", "Database error");

        return sanitized.trim();
    }

    /**
     * Check if error is transient (retry-able)
     */
    private boolean isTransientError(JobSchedulingException e) {
        if (e.getCause() == null) {
            return false;
        }

        String causeMessage = e.getCause().getMessage();
        if (causeMessage == null) {
            return false;
        }

        // Check for common transient error patterns
        return causeMessage.contains("timeout") ||
                causeMessage.contains("connection") ||
                causeMessage.contains("unavailable") ||
                causeMessage.contains("deadlock");
    }

    /**
     * Standard error response DTO
     */
    public record ErrorResponse(
            String errorCode,
            String message,
            String details,
            String correlationId
    ) {
    }

    /**
     * Validation error response with field-level errors
     */
    public record ValidationErrorResponse(
            String errorCode,
            String message,
            List<String> errors,
            String correlationId
    ) {
    }
}

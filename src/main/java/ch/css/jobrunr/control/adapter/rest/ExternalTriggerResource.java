package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.adapter.rest.dto.BatchProgressDTO;
import ch.css.jobrunr.control.adapter.rest.dto.JobStatusResponse;
import ch.css.jobrunr.control.adapter.rest.dto.TriggerJobResponse;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionByIdUseCase;
import ch.css.jobrunr.control.application.scheduling.ExecuteJobImmediatelyUseCase;
import ch.css.jobrunr.control.domain.BatchProgress;
import ch.css.jobrunr.control.domain.JobExecutionInfo;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * REST API for triggering and monitoring externally triggerable jobs.
 * This API is intended for external systems to trigger scheduled jobs
 * and check their execution status.
 */
@Path("/api/external-trigger")
@Tag(name = "External Trigger", description = "API for triggering and monitoring externally triggerable jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalTriggerResource {

    private static final Logger log = LoggerFactory.getLogger(ExternalTriggerResource.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ExecuteJobImmediatelyUseCase executeJobUseCase;
    private final GetJobExecutionByIdUseCase getJobExecutionByIdUseCase;

    @Inject
    public ExternalTriggerResource(
            ExecuteJobImmediatelyUseCase executeJobUseCase,
            GetJobExecutionByIdUseCase getJobExecutionByIdUseCase) {
        this.executeJobUseCase = executeJobUseCase;
        this.getJobExecutionByIdUseCase = getJobExecutionByIdUseCase;
    }

    /**
     * Triggers an externally triggerable job immediately.
     *
     * @param jobId The UUID of the scheduled job to trigger
     * @return Response containing the triggered job ID and message
     */
    @POST
    @Path("/{jobId}/trigger")
    @PermitAll
    @Operation(
            summary = "Trigger a job",
            description = "Triggers an externally triggerable job immediately. The job must be scheduled with external trigger flag."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Job triggered successfully",
                    content = @Content(schema = @Schema(implementation = TriggerJobResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Job not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response triggerJob(
            @Parameter(description = "Job ID", required = true)
            @PathParam("jobId") UUID jobId) {

        log.info("Triggering job with ID: {}", jobId);

        try {
            executeJobUseCase.execute(jobId);

            TriggerJobResponse response = new TriggerJobResponse(
                    jobId,
                    "Job triggered successfully"
            );

            log.info("Job {} triggered successfully", jobId);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid job ID: {}", jobId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid job ID: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Error triggering job {}", jobId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to trigger job: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Gets the status of a job execution.
     *
     * @param jobId The UUID of the job execution to check
     * @return Response containing job status and progress information
     */
    @GET
    @Path("/{jobId}/status")
    @PermitAll
    @Operation(
            summary = "Get job status",
            description = "Returns the current status of a job execution. If the job is a batch job, includes progress information."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Job status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = JobStatusResponse.class))
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Job not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response getJobStatus(
            @Parameter(description = "Job ID", required = true)
            @PathParam("jobId") UUID jobId) {

        log.debug("Getting status for job: {}", jobId);

        try {
            JobExecutionInfo executionInfo = getJobExecutionByIdUseCase.execute(jobId);

            BatchProgressDTO batchProgressDTO = null;
            if (executionInfo.getBatchProgress().isPresent()) {
                BatchProgress progress = executionInfo.getBatchProgress().get();
                batchProgressDTO = new BatchProgressDTO(
                        progress.getTotal(),
                        progress.getSucceeded(),
                        progress.getFailed(),
                        progress.getPending(),
                        progress.getProgress()
                );
            }

            JobStatusResponse response = new JobStatusResponse(
                    executionInfo.getJobId().toString(),
                    executionInfo.getJobName(),
                    executionInfo.getJobType(),
                    executionInfo.getStatus(),
                    executionInfo.getStartedAt() != null ? ISO_FORMATTER.format(executionInfo.getStartedAt()) : null,
                    executionInfo.getFinishedAt().map(ISO_FORMATTER::format).orElse(null),
                    batchProgressDTO
            );

            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            log.error("Invalid job ID: {}", jobId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid job ID: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.error("Error getting status for job {}", jobId, e);

            // Check if it's a not found exception
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("Job not found: " + jobId))
                        .build();
            }

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to get job status: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Error response record for consistent error handling.
     */
    private record ErrorResponse(String message) {
    }
}

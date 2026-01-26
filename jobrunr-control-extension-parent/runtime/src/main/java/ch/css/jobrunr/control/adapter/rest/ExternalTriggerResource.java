package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.adapter.rest.dto.BatchProgressDTO;
import ch.css.jobrunr.control.adapter.rest.dto.CloneAndStartJobRequestDTO;
import ch.css.jobrunr.control.adapter.rest.dto.JobStatusResponse;
import ch.css.jobrunr.control.adapter.rest.dto.TriggerJobResponse;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionByIdUseCase;
import ch.css.jobrunr.control.application.scheduling.CloneAndStartJobUseCase;
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
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * REST API for triggering and monitoring externally triggerable jobs.
 * This API is intended for external systems to trigger scheduled jobs
 * and check their execution status.
 * <p>
 * Note: The base path can be configured via jobrunr.control.api.basePath
 * Additional programmatic routes are registered in deployment module.
 */
@Path("/q/jobrunr-control/api/jobs")
@Tag(name = "External Trigger", description = "API for triggering and monitoring externally triggerable jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalTriggerResource {

    private static final Logger log = Logger.getLogger(ExternalTriggerResource.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ExecuteJobImmediatelyUseCase executeJobUseCase;
    private final GetJobExecutionByIdUseCase getJobExecutionByIdUseCase;
    private final CloneAndStartJobUseCase cloneAndStartJobUseCase;

    @Inject
    public ExternalTriggerResource(
            ExecuteJobImmediatelyUseCase executeJobUseCase,
            GetJobExecutionByIdUseCase getJobExecutionByIdUseCase,
            CloneAndStartJobUseCase cloneAndStartJobUseCase) {
        this.executeJobUseCase = executeJobUseCase;
        this.getJobExecutionByIdUseCase = getJobExecutionByIdUseCase;
        this.cloneAndStartJobUseCase = cloneAndStartJobUseCase;
    }

    /**
     * Starts an externally triggerable job immediately.
     *
     * @param jobId      Job ID from path parameter
     * @param parameters Optional map of parameter overrides in request body
     * @return Response containing the triggered job ID and message
     */
    @POST
    @Path("/{jobId}/start")
    @PermitAll
    @Operation(
            summary = "Start a job",
            description = "Starts an externally triggerable job immediately with optional parameter overrides. The job must be scheduled with external trigger flag."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Job triggered successfully",
                    content = @Content(schema = @Schema(implementation = TriggerJobResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request"
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
    public Response startJob(
            @Parameter(description = "Job ID", required = true)
            @PathParam("jobId") UUID jobId,
            @RequestBody(description = "Optional parameter overrides", required = false,
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
            java.util.Map<String, Object> parameters) {

        if (jobId == null) {
            log.errorf("Invalid request: jobId is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("jobId is required"))
                    .build();
        }

        log.infof("Starting job with ID: %s with %s parameter override(s)",
                jobId,
                parameters != null ? parameters.size() : 0);

        try {
            executeJobUseCase.execute(jobId, parameters);

            TriggerJobResponse response = new TriggerJobResponse(
                    jobId,
                    "Job started successfully"
            );

            log.infof("Job %s started successfully", jobId);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            log.errorf("Invalid job ID: %s", jobId, e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid job ID: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.errorf("Error starting job %s", jobId, e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to start job: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Clones an existing job and starts it immediately.
     *
     * @param request Request containing cloneFromId and optional parameter overrides
     * @return Response containing the new job ID and message
     */
    @POST
    @PermitAll
    @Operation(
            summary = "Clone and start a job",
            description = "Clones an existing scheduled job and starts it immediately with optional parameter overrides."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Job cloned and started successfully",
                    content = @Content(schema = @Schema(implementation = TriggerJobResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Source job not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response cloneAndStartJob(
            @RequestBody(description = "Clone and start job request with cloneFromId and optional parameters", required = true,
                    content = @Content(schema = @Schema(implementation = CloneAndStartJobRequestDTO.class)))
            CloneAndStartJobRequestDTO request) {

        if (request == null || request.cloneFromId() == null) {
            log.errorf("Invalid request: cloneFromId is required");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("cloneFromId is required"))
                    .build();
        }

        log.infof("Cloning and starting job with ID: %s with %s parameter override(s)",
                request.cloneFromId(),
                request.parameters() != null ? request.parameters().size() : 0);

        try {
            UUID newJobId = cloneAndStartJobUseCase.execute(request.cloneFromId(), request.suffix(), request.parameters());

            TriggerJobResponse response = new TriggerJobResponse(
                    newJobId,
                    "Job cloned and started successfully"
            );

            log.infof("Job cloned and started successfully with new ID: %s", newJobId);
            return Response.ok(response).build();

        } catch (IllegalArgumentException e) {
            log.errorf("Invalid request: %s", e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.errorf("Error cloning and starting job %s", request.cloneFromId(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to clone and start job: " + e.getMessage()))
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
    @Path("/{jobId}")
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

        log.debugf("Getting status for job: %s", jobId);

        try {
            JobExecutionInfo executionInfo = getJobExecutionByIdUseCase.execute(jobId);

            BatchProgressDTO batchProgressDTO = getBatchProgressDTO(executionInfo);

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
            log.errorf(e, "Invalid job ID: %s", jobId);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid job ID: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            log.errorf(e, "Error getting status for job %s", jobId);

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

    private static BatchProgressDTO getBatchProgressDTO(JobExecutionInfo executionInfo) {
        BatchProgressDTO batchProgressDTO = null;
        if (executionInfo.getBatchProgress().isPresent()) {
            BatchProgress progress = executionInfo.getBatchProgress().get();
            batchProgressDTO = new BatchProgressDTO(
                    progress.total(),
                    progress.succeeded(),
                    progress.failed(),
                    progress.getPending(),
                    progress.getProgress()
            );
        }
        return batchProgressDTO;
    }

    /**
     * Error response record for consistent error handling.
     */
    private record ErrorResponse(String message) {
    }
}

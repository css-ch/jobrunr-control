package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.adapter.rest.dto.BatchProgressDTO;
import ch.css.jobrunr.control.adapter.rest.dto.JobStatusResponse;
import ch.css.jobrunr.control.adapter.rest.dto.StartJobRequestDTO;
import ch.css.jobrunr.control.adapter.rest.dto.StartJobResponse;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionByIdUseCase;
import ch.css.jobrunr.control.application.scheduling.StartJobUseCase;
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
 * REST API for controlling and monitoring jobs.
 * This API is intended for external systems to start scheduled jobs
 * and check their execution status.
 * <p>
 * Note: The base path can be configured via jobrunr.control.api.basePath
 * Additional programmatic routes are registered in deployment module.
 */
@Path("/q/jobrunr-control/api")
@Tag(name = "Job Control", description = "API for controlling and monitoring jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobControlResource {

    private static final Logger LOG = Logger.getLogger(JobControlResource.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final StartJobUseCase startJobUseCase;
    private final GetJobExecutionByIdUseCase getJobExecutionByIdUseCase;

    @Inject
    public JobControlResource(
            StartJobUseCase startJobUseCase,
            GetJobExecutionByIdUseCase getJobExecutionByIdUseCase) {
        this.startJobUseCase = startJobUseCase;
        this.getJobExecutionByIdUseCase = getJobExecutionByIdUseCase;
    }

    /**
     * Starts a job (regular or template) immediately.
     * If the job is a template, it will be cloned first with an optional postfix, then started.
     * If it's a regular job, it will be started directly (postfix is ignored).
     *
     * @param jobId   Job ID from path parameter
     * @param request Request body containing optional postfix and parameter overrides
     * @return Response containing the started job ID and message
     */
    @POST
    @Path("jobs/{jobId}/start")
    @PermitAll
    @Operation(
            summary = "Start a job",
            description = "Starts a job immediately. If the job is a template, it will be cloned and started. " +
                    "If it's a regular scheduled job, it will be started directly. " +
                    "Optionally accepts a postfix for template job names and parameter overrides."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Job started successfully",
                    content = @Content(schema = @Schema(implementation = StartJobResponse.class))
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
            @RequestBody(description = "Optional postfix and parameter overrides", required = false,
                    content = @Content(schema = @Schema(implementation = StartJobRequestDTO.class)))
            StartJobRequestDTO request) {

        if (jobId == null) {
            LOG.errorf("Invalid request: jobId is required");
            throw new BadRequestException("jobId is required");
        }

        String postfix = request != null ? request.postfix() : null;
        java.util.Map<String, Object> parameters = request != null ? request.parameters() : null;

        LOG.infof("Starting job with ID: %s, postfix: %s, with %s parameter override(s)",
                jobId,
                postfix != null ? postfix : "none",
                parameters != null ? parameters.size() : 0);

        UUID resultJobId = startJobUseCase.execute(jobId, postfix, parameters);

        // Determine if this was a template (ID changed) or a regular job (ID stayed the same)
        boolean wasTemplate = !resultJobId.equals(jobId);
        String message = wasTemplate ? "Template job started successfully" : "Job started successfully";

        StartJobResponse response = new StartJobResponse(
                resultJobId,
                message
        );

        LOG.infof("Job started with ID: %s", resultJobId);
        return Response.ok(response).build();
    }

    /**
     * Gets the status of a job execution.
     *
     * @param jobId The UUID of the job execution to check
     * @return Response containing job status and progress information
     */
    @GET
    @Path("/jobs/{jobId}")
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

        LOG.debugf("Getting status for job: %s", jobId);

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
}

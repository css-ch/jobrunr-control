package ch.css.jobrunr.control.adapter.rest;

import ch.css.jobrunr.control.adapter.rest.dto.BatchProgressDTO;
import ch.css.jobrunr.control.adapter.rest.dto.JobStatusResponse;
import ch.css.jobrunr.control.adapter.rest.dto.StartJobResponse;
import ch.css.jobrunr.control.adapter.rest.dto.StartTemplateRequestDTO;
import ch.css.jobrunr.control.application.monitoring.GetJobExecutionByIdUseCase;
import ch.css.jobrunr.control.application.scheduling.ExecuteScheduledJobUseCase;
import ch.css.jobrunr.control.application.template.ExecuteTemplateUseCase;
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

    private static final Logger log = Logger.getLogger(JobControlResource.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final ExecuteScheduledJobUseCase executeScheduledJobUseCase;
    private final GetJobExecutionByIdUseCase getJobExecutionByIdUseCase;
    private final ExecuteTemplateUseCase executeTemplateUseCase;

    @Inject
    public JobControlResource(
            ExecuteScheduledJobUseCase executeScheduledJobUseCase,
            GetJobExecutionByIdUseCase getJobExecutionByIdUseCase,
            ExecuteTemplateUseCase executeTemplateUseCase) {
        this.executeScheduledJobUseCase = executeScheduledJobUseCase;
        this.getJobExecutionByIdUseCase = getJobExecutionByIdUseCase;
        this.executeTemplateUseCase = executeTemplateUseCase;
    }

    /**
     * Starts an externally triggerable job immediately.
     *
     * @param jobId      Job ID from path parameter
     * @param parameters Optional map of parameter overrides in request body
     * @return Response containing the started job ID and message
     */
    @POST
    @Path("jobs/{jobId}/start")
    @PermitAll
    @Operation(
            summary = "Start a job",
            description = "Starts an externally triggerable job immediately with optional parameter overrides. The job must be scheduled with external trigger flag."
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
            @RequestBody(description = "Optional parameter overrides", required = false,
                    content = @Content(schema = @Schema(implementation = java.util.Map.class)))
            java.util.Map<String, Object> parameters) {

        if (jobId == null) {
            log.errorf("Invalid request: jobId is required");
            throw new BadRequestException("jobId is required");
        }

        log.infof("Starting job with ID: %s with %s parameter override(s)",
                jobId,
                parameters != null ? parameters.size() : 0);

        executeScheduledJobUseCase.execute(jobId, parameters);

        StartJobResponse response = new StartJobResponse(
                jobId,
                "Job started successfully"
        );

        log.infof("Job %s started successfully", jobId);
        return Response.ok(response).build();
    }

    /**
     * Starts a template job by cloning it and executing the clone.
     *
     * @param templateId Template job ID from path parameter
     * @param request    Request body containing optional postfix and parameter overrides
     * @return Response containing the started job ID and message
     */
    @POST
    @Path("/templates/{templateId}/start")
    @PermitAll
    @Operation(
            summary = "Start a template job",
            description = "Starts a template job by cloning it and executing the clone immediately. Optionally accepts a postfix for the job name and parameter overrides."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Template job started successfully",
                    content = @Content(schema = @Schema(implementation = StartJobResponse.class))
            ),
            @APIResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Template job not found"
            ),
            @APIResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    public Response startTemplate(
            @Parameter(description = "Template Job ID", required = true)
            @PathParam("templateId") UUID templateId,
            @RequestBody(description = "Optional postfix and parameter overrides", required = false,
                    content = @Content(schema = @Schema(implementation = StartTemplateRequestDTO.class)))
            StartTemplateRequestDTO request) {

        if (templateId == null) {
            log.errorf("Invalid request: templateId is required");
            throw new BadRequestException("templateId is required");
        }

        String postfix = request != null ? request.postfix() : null;
        java.util.Map<String, Object> parameters = request != null ? request.parameters() : null;

        log.infof("Starting template job with ID: %s, postfix: %s, with %s parameter override(s)",
                templateId,
                postfix != null ? postfix : "auto-generated",
                parameters != null ? parameters.size() : 0);

        UUID newJobId = executeTemplateUseCase.execute(templateId, postfix, parameters);

        StartJobResponse response = new StartJobResponse(
                newJobId,
                "Template job started successfully"
        );

        log.infof("Template job %s cloned and started as job %s", templateId, newJobId);
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

        log.debugf("Getting status for job: %s", jobId);

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

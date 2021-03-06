package org.coner.core.resource;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.coner.core.api.entity.RunApiEntity;
import org.coner.core.api.request.AddRawTimeToFirstRunLackingRequest;
import org.coner.core.api.request.AddRunRequest;
import org.coner.core.api.response.GetEventRunsResponse;
import org.coner.core.domain.entity.Run;
import org.coner.core.domain.payload.RunAddPayload;
import org.coner.core.domain.payload.RunAddTimePayload;
import org.coner.core.domain.payload.RunTimeAddedPayload;
import org.coner.core.domain.service.RunEntityService;
import org.coner.core.domain.service.exception.AddEntityException;
import org.coner.core.domain.service.exception.EntityMismatchException;
import org.coner.core.domain.service.exception.EntityNotFoundException;
import org.coner.core.mapper.RunMapper;
import org.coner.core.util.swagger.ApiResponseConstants;
import org.coner.core.util.swagger.ApiTagConstants;
import org.eclipse.jetty.http.HttpStatus;

import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.validation.ValidationErrorMessage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ResponseHeader;

@Path("/events/{eventId}/runs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(tags = {ApiTagConstants.EVENTS, ApiTagConstants.RUNS})
public class EventRunsResource {

    private final RunEntityService runEntityService;
    private final RunMapper runMapper;

    @Inject
    public EventRunsResource(RunEntityService runEntityService, RunMapper runMapper) {
        this.runEntityService = runEntityService;
        this.runMapper = runMapper;
    }

    @POST
    @UnitOfWork
    @ApiOperation(value = "Add a new run")
    @ApiResponses({
            @ApiResponse(
                    code = HttpStatus.CREATED_201,
                    message = ApiResponseConstants.Created.MESSAGE,
                    responseHeaders = {
                            @ResponseHeader(
                                    name = ApiResponseConstants.Created.Headers.NAME,
                                    description = ApiResponseConstants.Created.Headers.DESCRIPTION,
                                    response = String.class
                            )
                    }
            ),
            @ApiResponse(
                    code = HttpStatus.NOT_FOUND_404,
                    response = ErrorMessage.class,
                    message = "No event with given ID"
            ),
            @ApiResponse(
                    code = HttpStatus.UNPROCESSABLE_ENTITY_422,
                    response = ValidationErrorMessage.class,
                    message = "Failed validation"
            )
    })
    public Response addRun(
            @PathParam("eventId") @ApiParam(value = "Event ID", required = true) String eventId,
            @Valid @ApiParam(value = "Run", required = true) AddRunRequest request
    ) throws AddEntityException, EntityNotFoundException {
        RunAddPayload addPayload = runMapper.toDomainAddPayload(request, eventId);
        Run domainEntity = runEntityService.add(addPayload);
        RunApiEntity run = runMapper.toApiEntity(domainEntity);
        return Response.created(UriBuilder.fromPath("/events/{eventId}/runs/{runId}")
                                        .build(eventId, run.getId()))
                .build();
    }

    @POST
    @Path("/rawTimes")
    @UnitOfWork
    @ApiOperation(
            value = "Add a raw time to the first run in sequence lacking one, "
                    + "or to a new run created on-the-fly if no runs lack a raw time"
    )
    @ApiResponses({
            @ApiResponse(
                    code = HttpStatus.OK_200,
                    message = "Assigned the given raw time to an existing run which was the first in sequence "
                            + "lacking one already",

                    response = RunApiEntity.class
            ),
            @ApiResponse(
                    code = HttpStatus.CREATED_201,
                    message = "Created a new run entity with the given raw time. From the perspective of this "
                            + "service, this isn't strictly an error, but would probably only come about due to an "
                            + "exceptional circumstance which may spell trouble for event operations, such as a "
                            + "false start/stop trip, a driver starting without the knowledge of the workers, etc.",
                    responseHeaders = {
                            @ResponseHeader(
                                    name = ApiResponseConstants.Created.Headers.NAME,
                                    description = ApiResponseConstants.Created.Headers.DESCRIPTION
                            )
                    },
                    response = RunApiEntity.class
            ),
            @ApiResponse(
                    code = HttpStatus.NOT_FOUND_404,
                    response = ErrorMessage.class,
                    message = "No event with given ID"
            ),
            @ApiResponse(
                    code = HttpStatus.UNPROCESSABLE_ENTITY_422,
                    response = ValidationErrorMessage.class,
                    message = "Failed validation"
            )
    })
    public Response addRawTimeToFirstRunInSequenceLackingOne(
            @PathParam("eventId") @ApiParam(value = "Event ID", required = true) String eventId,
            @Valid @ApiParam(value = "Time", required = true) AddRawTimeToFirstRunLackingRequest request
    ) throws AddEntityException, EntityNotFoundException {
        RunAddTimePayload inPayload = runMapper.toDomainAddTimePayload(request, eventId);
        RunTimeAddedPayload outPayload = runEntityService.addTimeToFirstRunInSequenceWithoutRawTime(inPayload);
        RunApiEntity run = runMapper.toApiEntity(outPayload.getRun());
        switch (outPayload.getOutcome()) {
            case RUN_RAWTIME_ASSIGNED_TO_EXISTING:
                return Response.ok(run, MediaType.APPLICATION_JSON).build();
            case RUN_ADDED_WITH_RAWTIME:
                return Response.created(UriBuilder.fromPath("/events/{eventId}/runs/{runId}")
                                                .build(eventId, run.getId()))
                        .entity(run)
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            default:
                throw new RuntimeException("Unknown outcome: " + outPayload.getOutcome());
        }
    }

    @GET
    @Path("/{runId}")
    @UnitOfWork
    @ApiOperation(value = "Get a specific run")
    @ApiResponses({
            @ApiResponse(code = HttpStatus.OK_200, response = RunApiEntity.class, message = "OK"),
            @ApiResponse(code = HttpStatus.NOT_FOUND_404, response = ErrorMessage.class, message = "Not found"),
            @ApiResponse(
                    code = HttpStatus.CONFLICT_409,
                    response = ErrorMessage.class,
                    message = "Event ID and Run ID are mismatched"
            )
    })
    public RunApiEntity getRun(
            @PathParam("eventId") @ApiParam(value = "Event ID", required = true) String eventId,
            @PathParam("runId") @ApiParam(value = "Run ID", required = true) String runId
    ) throws EntityMismatchException, EntityNotFoundException {
        Run domainRun = runEntityService.getByEventIdAndRunId(eventId, runId);
        return runMapper.toApiEntity(domainRun);
    }

    @GET
    @UnitOfWork
    @ApiOperation(
            value = "Get a list of all runs at an event",
            response = GetEventRunsResponse.class
    )
    @ApiResponses({
            @ApiResponse(
                    code = HttpStatus.OK_200,
                    message = "Success",
                    response = GetEventRunsResponse.class
            ),
            @ApiResponse(
                    code = HttpStatus.NOT_FOUND_404,
                    message = "No event with given ID",
                    response = ErrorMessage.class
            )
    })
    public GetEventRunsResponse getEventRuns(
            @PathParam("eventId") @ApiParam(value = "Event ID", required = true) String eventId
    ) throws EntityNotFoundException {
        List<Run> domainEntities = runEntityService.getAllWithEventId(eventId);
        GetEventRunsResponse response = new GetEventRunsResponse();
        response.setEntities(runMapper.toApiEntityList(domainEntities));
        return response;
    }

}

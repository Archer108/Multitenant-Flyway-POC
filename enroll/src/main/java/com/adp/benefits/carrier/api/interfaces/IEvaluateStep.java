package com.adp.benefits.carrier.api.interfaces;

import com.adp.benefits.carrier.exceptions.ErrorResponse;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Flux;

@Tag(name = "Evaluate Step API", description = "API for Evaluating Process Steps")
@RequestMapping(path = "/v1/api")
public interface IEvaluateStep {

    @Operation(
            summary = "Evaluate a step in the process",
            description =
                    "Streams evaluation lifecycle events (initiated → completed/timeout/failed).")
    @ApiResponses({
        @ApiResponse(
                responseCode = "202",
                description = "Request accepted; streaming evaluation events",
                content =
                        @Content(
                                mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                                schema = @Schema(implementation = EvaluateStepResponse.class))),
        @ApiResponse(
                responseCode = "500",
                description = "Internal Server Error",
                content =
                        @Content(
                                mediaType = MediaType.APPLICATION_JSON_VALUE,
                                schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(
            path = "/evaluateStep",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<Flux<ServerSentEvent<EvaluateStepResponse>>> evaluateStep(
            @RequestBody EvaluateStepRequest request);
}

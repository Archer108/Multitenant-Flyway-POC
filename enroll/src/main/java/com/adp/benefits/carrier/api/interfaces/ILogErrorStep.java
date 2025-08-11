package com.adp.benefits.carrier.api.interfaces;

import com.adp.benefits.carrier.model.cpm.models.LogErrorStepRequest;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Tag(
        name = "Log Step API",
        description = "API for logging process/system steps or errors generically from clients.")
@RequestMapping(value = "/v1/api")
public interface ILogErrorStep {

    @Operation(
            summary = "Logs a process/system step or error",
            description = "Accepts a generic error or step log from any client system.")
    @ApiResponse(
            responseCode = "200",
            description = "Log accepted and processed successfully.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = LogErrorStepResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request.",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
    @PostMapping(
            value = "/logStep",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<ResponseEntity<LogErrorStepResponse>> logStep(@RequestBody LogErrorStepRequest request);
}

package com.adp.benefits.carrier.api.interfaces;

import com.adp.benefits.carrier.exceptions.ErrorResponse;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import reactor.core.publisher.Mono;

@Tag(name = "Update Step API", description = "API for updating process step outcome")
@RequestMapping("/v1/api")
public interface IUpdateStep {

    @Operation(
            summary = "Updates a process step outcome",
            description = "Updates a process step outcome with the provided message")
    @ApiResponse(
            responseCode = "200",
            description = "OK. Update completed successfully",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateStepResponse.class)))
    @ApiResponse(
            responseCode = "400",
            description = "Bad Request",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(
            responseCode = "500",
            description = "Internal Server Error",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(
            value = "/updateStep",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    Mono<UpdateStepResponse> updateStep(@RequestBody UpdateStepRequest request);
}

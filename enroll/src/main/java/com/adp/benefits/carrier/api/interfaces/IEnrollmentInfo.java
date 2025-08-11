package com.adp.benefits.carrier.api.interfaces;

import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoPageEnvelope;
import com.adp.benefits.carrier.model.enrollment.EnrollmentInfoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Tag(name = "Enrollment API", description = "Cursor-based paged enrollment info.")
@RequestMapping("/v1/api")
public interface IEnrollmentInfo {

    @Operation(
            summary = "Get enrollment page (cursor-based)",
            description = "Returns a page of enrollment rows plus nextCursor and dynamic dropdown options.")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = EnrollmentInfoPageEnvelope.class)))
    @GetMapping(value = "/enrollmentInfo", produces = MediaType.APPLICATION_JSON_VALUE)
    Mono<ResponseEntity<EnrollmentInfoPageEnvelope>> getEnrollmentInfo(
            @RequestParam(name = "timeframe", defaultValue = "ONE_WEEK") String timeframe,
            @ModelAttribute EnrollmentInfoRequest.Duration duration,
            @RequestParam(name = "state",   required = false) String state,
            @RequestParam(name = "status",  required = false) String status,
            @RequestParam(name = "systemType", required = false) java.util.List<String> systemTypes,
            @RequestParam(name = "client",  required = false) String clientId,
            @RequestParam(name = "carrier", required = false) String carrierId,
            @RequestParam(name = "associateSearch", required = false) String associateSearch,
            @RequestParam(name = "globalSearch",    required = false) String globalSearch,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "size",   required = false) Integer size
    );
}
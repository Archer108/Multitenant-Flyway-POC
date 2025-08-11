package com.adp.benefits.carrier.model.enrollment;

import com.adp.benefits.carrier.model.cpm.enums.State;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentInfoRequest {

    @Schema(description = "Process state filter.")
    private State state;

    @Schema(description = "Timeframe for the query.")
    private Timeframe timeframe;

    @Schema(description = "Custom date range, used only when timeframe=CUSTOM_DATE_RANGE.")
    private Duration duration;

    @Schema(description = "Single-select client id (dropdown). Empty/null = All clients.")
    private String clientId;

    @Schema(description = "Single-select carrier id (dropdown). Empty/null = All carriers.")
    private String carrierId;

    @Schema(description = "Global search across AOID, associate name, carrier, state, status.")
    private String globalSearch;

    @Schema(description = "Associate AOID or name (prefix/suffix wildcard search).")
    private String associateSearch;

    @Schema(description = "Status filter.")
    private String status;

    @Schema(description = "System types (e.g., ENROLLMENT).")
    private List<String> systemTypes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Duration {
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
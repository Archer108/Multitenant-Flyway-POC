package com.adp.benefits.carrier.model.cpm.models;

import com.adp.benefits.carrier.model.cpm.enums.Severity;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ErrorInfo {
    private String errorDetails;
    private LocalDateTime timestamp;
    private Severity severity;
    private JsonNode payload;
}

package com.adp.benefits.carrier.model.cpm.models;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class EvaluateStepRequest {
    private UUID correlationId;
    private UUID transmissionId;
    private String aoid;
    private String clientId;
    private String systemId;
    private String processId;
    private String processStepId;
    @JsonRawValue private JsonNode payload;
}

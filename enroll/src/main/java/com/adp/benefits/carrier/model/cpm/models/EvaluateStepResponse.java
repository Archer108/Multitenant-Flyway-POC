// com/adp/benefits/carrier/model/EvaluateStepResponse.java
package com.adp.benefits.carrier.model.cpm.models;

import com.adp.benefits.carrier.model.cpm.enums.AsyncStatus;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({
    "timestamp",
    "correlationId",
    "transmissionId",
    "asyncStatus",
    "status",
    "state",
    "messages",
    "nextStep",
    "payload"
})
public class EvaluateStepResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-ss'T'HH:mm:ss.SSSXXX")
    private Date timestamp;

    private UUID correlationId;
    private UUID transmissionId;

    private AsyncStatus asyncStatus;

    private Status status;
    private State state;

    private List<RuleError> messages;

    private String nextStep;
    private JsonNode payload;
}

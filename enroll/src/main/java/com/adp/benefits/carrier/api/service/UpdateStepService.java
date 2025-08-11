package com.adp.benefits.carrier.api.service;

import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.enums.RspRequestStage;
import com.adp.benefits.carrier.model.cpm.enums.AsyncStatus;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.UpdateStepResponse;
import com.adp.benefits.carrier.service.ClientMessageService;
import com.adp.benefits.carrier.service.ProcessStepOutcomeService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateStepService {

    private final ProcessStepOutcomeService processStepOutcomeService;
    private final ClientMessageService clientMessageService;

    private static final String RSP_RESOURCE_RESULT_FAILED_STATUS_CODE = "failed";

    @Transactional
    public UpdateStepResponse appendMessageToProcessStepOutcome(UpdateStepRequest request) {
        ProcessStepOutcome outcome =
                processStepOutcomeService.findOrCreateOutcome(
                        null,
                        request.getTransmissionId().toString(),
                        request.getOrgId(),
                        request.getProcessStepId(),
                        request.getAoid());

        log.info("Updating outcome with id: {}", outcome.getProcessStepOutcomeId());

        JsonNode payloadNode = request.getPayload();
        ParsedPayload parsedPayload = parsePayload(payloadNode);
        log.info(
                "Extracted requestStage: {}, resourceResultStatusCode: {}",
                parsedPayload.requestStage(),
                parsedPayload.resourceResultStatusCode());

        clientMessageService.saveMessage(outcome.getProcessStepOutcomeId(), payloadNode);

        if (parsedPayload.requestStage != null) {

            boolean isFailed =
                    parsedPayload.resourceResultStatusCode() != null
                            && parsedPayload.resourceResultStatusCode.equalsIgnoreCase(
                                    RSP_RESOURCE_RESULT_FAILED_STATUS_CODE);

            outcome.setStatus(Status.ASYNC);

            switch (parsedPayload.requestStage) {
                case CONFIG, TRANSFORM, VALIDATE:
                    if (isFailed) {
                        outcome.setState(State.ONE_CG_ERROR);
                    }
                    break;
                case SEND:
                    outcome.setState(isFailed ? State.ONE_CG_ERROR : State.ONE_CG_SEND);
                    break;
                case SYNCRSP, ASYNCRSP:
                    outcome.setState(isFailed ? State.CARRIER_ERROR : State.CARRIER_RECEIVED);
                    break;
                default:
                    log.warn(
                            "Unexpected request stage: {}. No state change applied.",
                            parsedPayload.requestStage());
            }
        } else {
            log.warn("Request state is null. No state change applied.");
        }

        outcome.setModifyDt(Date.from(Instant.now()));
        processStepOutcomeService.saveProcessStepOutcome(outcome);

        UUID correlationUuid = null;
        if (outcome.getCorrelationId() != null && !outcome.getCorrelationId().isEmpty()) {
            correlationUuid = UUID.fromString(outcome.getCorrelationId());
        }

        return UpdateStepResponse.builder()
                .timestamp(Date.from(Instant.now()))
                .correlationId(correlationUuid)
                .transmissionId(request.getTransmissionId())
                .asyncStatus(AsyncStatus.COMPLETED)
                .build();
    }

    private ParsedPayload parsePayload(JsonNode payloadNode) {
        RspRequestStage requestStage = null;
        String resourceResultStatusCode = null;

        if (payloadNode.has("requestStage")) {
            requestStage = RspRequestStage.fromString(payloadNode.get("requestStage").asText());
        }
        if (payloadNode.has("requestResultStatusCode")) {
            resourceResultStatusCode = payloadNode.get("requestResultStatusCode").asText();
        }

        return new ParsedPayload(requestStage, resourceResultStatusCode);
    }

    private record ParsedPayload(RspRequestStage requestStage, String resourceResultStatusCode) {}
}

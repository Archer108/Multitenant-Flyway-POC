package com.adp.benefits.carrier.api.service;

import static com.adp.benefits.carrier.util.BcpmGlobalConstants.MISCELLANEOUS_CARRIER;

import com.adp.benefits.carrier.entity.client.EnrollmentInfo;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.EnrollmentInfoRepository;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepRequest;
import com.adp.benefits.carrier.model.cpm.models.LogErrorStepResponse;
import com.adp.benefits.carrier.service.ClientMessageService;
import com.adp.benefits.carrier.service.ProcessStepOutcomeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class LogErrorStepService {

    private final ProcessStepOutcomeService processStepOutcomeService;
    private final ClientMessageService clientMessageService;
    private final ObjectMapper objectMapper;
    private final EnrollmentInfoRepository enrollmentInfoRepository;

    @Transactional
    public LogErrorStepResponse appendErrorMessagesToProcessStepOutcome(
            LogErrorStepRequest request) {

        String tid =
                request.getTransmissionId() != null ? request.getTransmissionId().toString() : null;

        // find or create the step outcome
        ProcessStepOutcome outcome =
                processStepOutcomeService.findOrCreateOutcome(
                        null,
                        tid,
                        request.getOrgId(),
                        request.getProcessStepId(),
                        request.getAoid());

        log.info("Updating outcome {}", outcome);

        JsonNode errorAsJsonNode = objectMapper.valueToTree(request.getErrorInfo());

        clientMessageService.saveMessage(outcome.getProcessStepOutcomeId(), errorAsJsonNode);

        outcome.setStatus(Status.INTERNAL_SERVER_ERROR);
        outcome.setState(State.CRITICAL_ERROR); // TODO: Check what to update here?
        outcome.setModifyDt(Date.from(Instant.now()));
        processStepOutcomeService.saveProcessStepOutcome(outcome);
        persistEnrollmentInfo(outcome);

        return LogErrorStepResponse.builder()
                .timestamp(Date.from(Instant.now()))
                .success(true)
                .build();
    }

    private void persistEnrollmentInfo(ProcessStepOutcome outcome) {
        try {
            log.debug(
                    "Persisting enrollment info for processStepOutcomeId: {}",
                    outcome.getProcessStepOutcomeId());

            if (enrollmentInfoRepository.findByProcessStepOutcome(outcome) != null) {
                log.debug(
                        "Enrollment info already exists for processStepOutcomeId: {}",
                        outcome.getProcessStepOutcomeId());
                return;
            }

            EnrollmentInfo enrollmentInfo =
                    EnrollmentInfo.builder()
                            .enrollInfoId(UUID.randomUUID().toString())
                            .processStepOutcome(outcome)
                            .carrierId(MISCELLANEOUS_CARRIER)
                            .build();
            enrollmentInfoRepository.save(enrollmentInfo);

            log.info(
                    "Successfully persisted enrollment info with ID: {}",
                    enrollmentInfo.getEnrollInfoId());
        } catch (Exception e) {
            log.error(
                    "Error persisting enrollment info for processStepOutcomeId: {}",
                    outcome.getProcessStepOutcomeId(),
                    e);
            throw e;
        }
    }
}

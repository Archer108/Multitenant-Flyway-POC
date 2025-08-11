package com.adp.benefits.carrier.service;

import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.ProcessStepOutcomeRepository;
import com.adp.benefits.carrier.exceptions.DatabaseServiceOperationException;
import com.adp.benefits.carrier.model.cpm.enums.AsyncStatus;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessStepOutcomeService {

    private final ProcessStepOutcomeRepository outcomeRepo;
    private final RetryTemplate retryTemplate;

    public ProcessStepOutcome findOrCreateOutcome(
            String correlationId,
            String transmissionId,
            String clientId,
            String processStepId,
            String aoid) {
        try {
            // First try to find by transmissionId if it's not null
            if (transmissionId != null && !transmissionId.isEmpty()) {
                return outcomeRepo
                        .findByTransmissionId(transmissionId)
                        .orElseGet(
                                () ->
                                        createNewOutcome(
                                                correlationId,
                                                transmissionId,
                                                clientId,
                                                processStepId,
                                                aoid));
            }

            // If transmissionId is null or empty, try to find by correlationId
            if (correlationId != null && !correlationId.isEmpty()) {
                return outcomeRepo
                        .findByCorrelationId(correlationId)
                        .orElseGet(
                                () ->
                                        createNewOutcome(
                                                correlationId,
                                                transmissionId,
                                                clientId,
                                                processStepId,
                                                aoid));
            }

            // If both are null or empty, just create a new outcome
            return createNewOutcome(correlationId, transmissionId, clientId, processStepId, aoid);

        } catch (Exception e) {
            log.error(
                    "Database error while finding or creating outcome for correlationId={}, transmissionId={}: {}",
                    correlationId,
                    transmissionId,
                    e.getMessage(),
                    e);
            throw new DatabaseServiceOperationException("Failed to find or create outcome", e);
        }
    }

    private ProcessStepOutcome createNewOutcome(
            String correlationId,
            String transmissionId,
            String clientId,
            String processStepId,
            String aoid) {
        try {
            String corrId = correlationId != null ? correlationId : "";
            String transId = transmissionId != null ? transmissionId : "";

            ProcessStepOutcome outcome =
                    ProcessStepOutcome.builder()
                            .processStepOutcomeId(UUID.randomUUID().toString())
                            .correlationId(corrId)
                            .transmissionId(transId)
                            .clientId(clientId)
                            .processStepId(processStepId)
                            .status(Status.IN_PROGRESS)
                            .nextStepId("")
                            .resume(false) // Always false for new outcome
                            .createDt(Date.from(Instant.now()))
                            .modifyDt(Date.from(Instant.now()))
                            .aoid(aoid)
                            .build();

            return outcomeRepo.save(outcome);
        } catch (Exception e) {
            log.error(
                    "Database error while creating new outcome for correlationId={}, transmissionId={}: {}",
                    correlationId,
                    transmissionId,
                    e.getMessage(),
                    e);
            throw new DatabaseServiceOperationException("Failed to create new outcome", e);
        }
    }

    public void saveProcessStepOutcome(ProcessStepOutcome outcome) {
        try {
            retryTemplate.execute(
                    context -> {
                        log.debug(
                                "Attempting saveProcessStepOutcome[processStepOutcomeId={}, correlationId={}, transmissionId={}, processStepId={}, aoid={}] attempt={}",
                                outcome.getProcessStepOutcomeId(),
                                outcome.getCorrelationId(),
                                outcome.getTransmissionId(),
                                outcome.getProcessStepId(),
                                outcome.getAoid(),
                                context.getRetryCount());
                        return outcomeRepo.save(outcome);
                    });
        } catch (DatabaseServiceOperationException e) {
            throw e; // Re-throw your custom exception
        } catch (Exception e) {
            log.error(
                    "Unexpected error saving ProcessStepOutcome[processStepOutcomeId={}]: {}",
                    outcome.getProcessStepOutcomeId(),
                    e.getMessage(),
                    e);
            throw new DatabaseServiceOperationException("Error saving process step outcome", e);
        }
    }

    public void rollbackProcessStepOutcomeForTimeoutAndFailure(
            String cid, String tid, String processId, String processStepId, AsyncStatus status) {
        outcomeRepo
                .findByCorrelationIdAndTransmissionId(cid, tid)
                .ifPresentOrElse(
                        outcome -> {
                            try {
                                log.info(
                                        "Cleaning up process step outcome {}: (cid={} tid={} processId={} processStepId={}) due to AsyncStatus returning {}",
                                        outcome.getProcessStepOutcomeId(),
                                        cid,
                                        tid,
                                        processId,
                                        processStepId,
                                        status);
                                outcomeRepo.delete(outcome);
                            } catch (Exception e) {
                                log.error(
                                        "Database error while deleting outcome {}: {}",
                                        outcome.getProcessStepOutcomeId(),
                                        e.getMessage(),
                                        e);
                                throw new DatabaseServiceOperationException(
                                        "Failed to delete process step outcome", e);
                            }
                        },
                        () -> {
                            log.info(
                                    "No process step outcome found for cleanup: (cid={} tid={} processStepId={}, reason={}), skipping delete.",
                                    cid,
                                    tid,
                                    processStepId,
                                    status);
                        });
    }
}

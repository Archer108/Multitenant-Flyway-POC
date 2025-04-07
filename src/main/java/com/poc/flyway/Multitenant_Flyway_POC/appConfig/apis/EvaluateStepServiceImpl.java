package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.IEvaluateStepService;
import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.client.ActionContext;
import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.MessagesRepository;
import com.adp.benefits.carrier.entity.client.repository.ProcessStepOutcomeRepository;
import com.adp.benefits.carrier.entity.primary.*;
import com.adp.benefits.carrier.entity.primary.repository.*;
import com.adp.benefits.carrier.enums.Status;
import com.adp.benefits.carrier.model.EvaluateStepRequest;
import com.adp.benefits.carrier.model.EvaluateStepResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class EvaluateStepServiceImpl implements IEvaluateStepService {

    private final ProcessStepOutcomeRepository processStepOutcomeRepository;
    private final ProcessRepository processRepository;
    private final ProcessStepRepository processStepRepository;
    private final IRuleExecutionService ruleExecutionService;
    private final ProcStepAttrAssocRepository procStepAttrAssocRepository;
    private final ProcStepAttrRuleAssocRepository procStepAttrRuleAssocRepository;
    private final MessagesRepository messageRepository;

    public EvaluateStepServiceImpl(
            ProcessStepOutcomeRepository processStepOutcomeRepository,
            ProcessRepository processRepository,
            ProcessStepRepository processStepRepository,
            IRuleExecutionService ruleExecutionService,
            ProcStepAttrAssocRepository procStepAttrAssocRepository,
            ProcStepAttrRuleAssocRepository procStepAttrRuleAssocRepository,
            MessagesRepository messageRepository) {
        this.processStepOutcomeRepository = processStepOutcomeRepository;
        this.processRepository = processRepository;
        this.processStepRepository = processStepRepository;
        this.ruleExecutionService = ruleExecutionService;
        this.procStepAttrAssocRepository = procStepAttrAssocRepository;
        this.procStepAttrRuleAssocRepository = procStepAttrRuleAssocRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    @Override
    public EvaluateStepResponse evaluateStep(EvaluateStepRequest request) {
        log.info("▶️ Evaluating step for correlationId: {}", request.getCorrelationId());

        validateRequest(request);

        ProcessEntity processEntity = findProcess(request.getProcessId());
        ProcessStep processStep = findProcessStep(request.getProcessStepId());

        ProcessStepOutcome stepOutcome = findOrCreateProcessStepOutcome(request, processStep);

        if (isClientStepOverridden(request.getClientId(), processStep.getProcessStepId())) {
            finalizeSkippedStep(stepOutcome);
            return buildResponse(stepOutcome, Collections.emptyList());
        }

        storeActionContext(stepOutcome, request.getPayload());

        List<String> errorMessages = runAttributeRules(processStep, request.getPayload(), stepOutcome);

        stepOutcome.setStatus(errorMessages.isEmpty() ? Status.CONTINUE : Status.ERROR);
        stepOutcome.setNextStepId(determineNextStepId(errorMessages));
        stepOutcome.setUpdatedAt(new Date());
        processStepOutcomeRepository.save(stepOutcome);

        return buildResponse(stepOutcome, errorMessages);
    }

    // --- Helper Methods ---

    private void validateRequest(EvaluateStepRequest request) {
        Objects.requireNonNull(request.getCorrelationId(), "correlationId is required");
        Objects.requireNonNull(request.getTransmissionId(), "transmissionId is required");
        Objects.requireNonNull(request.getClientId(), "clientId is required");
        Objects.requireNonNull(request.getProcessId(), "processId is required");
        Objects.requireNonNull(request.getProcessStepId(), "processStepId is required");
    }

    private ProcessEntity findProcess(String processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found: " + processId));
    }

    private ProcessStep findProcessStep(String processStepId) {
        return processStepRepository.findById(processStepId)
                .orElseThrow(() -> new RuntimeException("ProcessStep not found: " + processStepId));
    }

    private ProcessStepOutcome findOrCreateProcessStepOutcome(EvaluateStepRequest request, ProcessStep processStep) {
        return processStepOutcomeRepository
                .findByCorrelationIdAndTransmissionIdAndProcessStepId(
                        request.getCorrelationId(), request.getTransmissionId(), request.getProcessStepId())
                .orElseGet(() -> {
                    ProcessStepOutcome newOutcome = ProcessStepOutcome.builder()
                            .processStepOutcomeId(UUID.randomUUID().toString())
                            .correlationId(request.getCorrelationId())
                            .transmissionId(request.getTransmissionId())
                            .clientId(request.getClientId())
                            .processStepId(processStep.getProcessStepId())
                            .status(Status.IN_PROGRESS)
                            .nextStepId("")
                            .createdAt(new Date())
                            .updatedAt(new Date())
                            .build();
                    return processStepOutcomeRepository.save(newOutcome);
                });
    }

    private boolean isClientStepOverridden(String clientId, String processStepId) {
        return "overrideClient".equals(clientId) && "overrideStep".equals(processStepId);
    }

    private void finalizeSkippedStep(ProcessStepOutcome stepOutcome) {
        stepOutcome.setStatus(Status.SKIPPED);
        stepOutcome.setNextStepId("");
        stepOutcome.setUpdatedAt(new Date());
        processStepOutcomeRepository.save(stepOutcome);
    }

    private void storeActionContext(ProcessStepOutcome stepOutcome, JsonNode payload) {
        String transmissionId = stepOutcome.getTransmissionId();
        stepOutcome.getActionContexts().stream()
                .filter(ac -> transmissionId.equals(ac.getTransmissionId()))
                .findFirst()
                .ifPresentOrElse(
                        ac -> ac.setPayload(payload),
                        () -> stepOutcome.getActionContexts().add(ActionContext.builder()
                                .transmissionId(transmissionId)
                                .processStepOutcomeId(stepOutcome.getProcessStepOutcomeId())
                                .payload(payload)
                                .build()));
    }

    private List<String> runAttributeRules(ProcessStep processStep, JsonNode payload, ProcessStepOutcome stepOutcome) {
        List<String> errorMessages = new ArrayList<>();

        List<ProcStepAttrAssoc> assocList = procStepAttrAssocRepository.findByProcessStepIdAndEnabledIn(
                processStep.getProcessStepId(), true);

        for (ProcStepAttrAssoc assoc : assocList) {
            List<ProcStepAttrRuleAssoc> ruleAssocs = procStepAttrRuleAssocRepository
                    .findByProcStepAttrAssocIdAndEnabledIn(assoc.getProcStepAttrAssocId(), true);

            for (ProcStepAttrRuleAssoc ruleAssoc : ruleAssocs) {
                Rule rule = ruleAssoc.getRule();

                log.info("🧠 Validating Rule [{}] - '{}' linked to AttrAssoc [{}]",
                        rule.getRuleId(), rule.getName(), assoc.getProcStepAttrAssocId());

                List<String> ruleErrors = ruleExecutionService.evaluate(rule, payload.toString());

                if (!ruleErrors.isEmpty()) {
                    ruleErrors.forEach(msg -> {
                        Message runtimeMsg = Message.builder()
                                .messageId(UUID.randomUUID().toString())
                                .processStepOutcomeId(stepOutcome.getProcessStepOutcomeId())
                                .message(msg)
                                .build();
                        messageRepository.save(runtimeMsg);
                        log.info("❌ Saved failure message for rule [{}]: {}", rule.getRuleId(), msg);
                    });
                    errorMessages.addAll(ruleErrors);
                } else {
                    log.info("✅ Rule [{}] passed validation", rule.getRuleId());
                }
            }
        }

        return errorMessages;
    }

    private String determineNextStepId(List<String> errorMessages) {
        return errorMessages.isEmpty() ? "some_next_step_id" : "some_fix_error_next_step_id";
    }

    private EvaluateStepResponse buildResponse(ProcessStepOutcome stepOutcome, List<String> messages) {
        return EvaluateStepResponse.builder()
                .correlationId(stepOutcome.getCorrelationId())
                .transmissionId(stepOutcome.getTransmissionId())
                .status(stepOutcome.getStatus())
                .messages(messages)
                .nextStep(stepOutcome.getNextStepId())
                .build();
    }
}
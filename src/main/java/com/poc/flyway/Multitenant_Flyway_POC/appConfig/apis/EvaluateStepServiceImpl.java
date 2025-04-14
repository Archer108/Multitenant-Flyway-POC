package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.IEvaluateStepService;
import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.api.service.RuleProcessingContext;
import com.adp.benefits.carrier.entity.client.ActionContext;
import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.MessagesRepository;
import com.adp.benefits.carrier.entity.client.repository.ProcessStepOutcomeRepository;
import com.adp.benefits.carrier.entity.primary.*;
import com.adp.benefits.carrier.entity.primary.repository.*;
import com.adp.benefits.carrier.enums.Status;
import com.adp.benefits.carrier.exceptions.ClientValidationException;
import com.adp.benefits.carrier.exceptions.ProcessStepValidationException;
import com.adp.benefits.carrier.model.EvaluateStepRequest;
import com.adp.benefits.carrier.model.EvaluateStepResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

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

        try {
            validateRequest(request);

        } catch (ClientValidationException ex) {
            log.error("Client Validation error: {}", ex.getMessage());
            return EvaluateStepResponse.builder()
                    .correlationId(request.getCorrelationId())
                    .transmissionId(request.getTransmissionId())
                    .status(Status.ERROR)
                    .messages(Collections.singletonList(ex.getMessage()))
                    .nextStep("")
                    .build();
        }

        ProcessStep processStep = findProcessStep(request.getProcessStepId());

        ProcessStepOutcome stepOutcome = findOrCreateProcessStepOutcome(request, processStep);

        if (isClientStepOverridden(request.getClientId(), processStep.getProcessStepId())) {
            finalizeSkippedStep(stepOutcome);
            return buildResponse(stepOutcome, Collections.emptyList());
        }

        storeActionContext(stepOutcome, request.getPayload());

        List<String> errorMessages =
                runAttributeRules(processStep, request.getPayload(), stepOutcome);

        stepOutcome.setStatus(errorMessages.isEmpty() ? Status.CONTINUE : Status.ERROR);
        stepOutcome.setNextStepId(determineNextStepId(processStep, errorMessages));
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
        Objects.requireNonNull(request.getSystemId(), "systemId is required");

        if (request.getPayload() == null
                || request.getPayload().isNull()
                || request.getPayload().isEmpty()) {
            throw new ClientValidationException("Payload is required.");
        }

        ProcessStep processStep =
                processStepRepository
                        .findById(request.getProcessStepId())
                        .orElseThrow(
                                () ->
                                        new ProcessStepValidationException(
                                                "Process Step Not Found: "
                                                        + request.getProcessStepId()));

        if (!request.getProcessId().equals(processStep.getProcessEntity().getProcessId())) {
            throw new ProcessStepValidationException(
                    "Process step "
                            + request.getProcessStepId()
                            + "does not belong to process "
                            + request.getProcessId());
        }

        if (!request.getSystemId()
                .equals(processStep.getProcessEntity().getSystemEntity().getSystemId())) {
            throw new ProcessStepValidationException(
                    "Process step "
                            + request.getProcessId()
                            + " does not belong to system "
                            + request.getSystemId());
        }
    }

    private ProcessStep findProcessStep(String processStepId) {
        return processStepRepository
                .findById(processStepId)
                .orElseThrow(() -> new RuntimeException("ProcessStep not found: " + processStepId));
    }

    private ProcessStepOutcome findOrCreateProcessStepOutcome(
            EvaluateStepRequest request, ProcessStep processStep) {
        return processStepOutcomeRepository
                .findByCorrelationIdAndTransmissionIdAndProcessStepId(
                        request.getCorrelationId(),
                        request.getTransmissionId(),
                        request.getProcessStepId())
                .orElseGet(
                        () -> {
                            ProcessStepOutcome newOutcome =
                                    ProcessStepOutcome.builder()
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
                        () ->
                                stepOutcome
                                        .getActionContexts()
                                        .add(
                                                ActionContext.builder()
                                                        .transmissionId(transmissionId)
                                                        .processStepOutcomeId(
                                                                stepOutcome
                                                                        .getProcessStepOutcomeId())
                                                        .payload(payload)
                                                        .build()));
    }

    private List<String> runAttributeRules(
            ProcessStep processStep, JsonNode payload, ProcessStepOutcome stepOutcome) {
        List<String> errorMessages = new ArrayList<>();
        Map<String, String> ruleExecutionTimes = new HashMap<>();
        Map<String, String> ruleStatuses = new HashMap<>();
        List<Map<String, String>> erroredRules = new ArrayList<>();
        AtomicLong totalExecutionTime = new AtomicLong(0);

        String payloadString = payload.toString();

        List<ProcStepAttrAssoc> assocList =
                procStepAttrAssocRepository.findByProcessStepIdAndEnabledIn(
                        processStep.getProcessStepId(), true);

        log.info("🧩 Found {} attribute associations for stepId {}", assocList.size(), processStep.getProcessStepId());

        RuleProcessingContext context =
                new RuleProcessingContext(
                        ruleExecutionTimes,
                        ruleStatuses,
                        erroredRules,
                        errorMessages,
                        totalExecutionTime,
                        stepOutcome);

        for (ProcStepAttrAssoc assoc : assocList) {
            List<ProcStepAttrRuleAssoc> ruleAssocs =
                    procStepAttrRuleAssocRepository.findByProcStepAttrAssocIdAndEnabledIn(
                            assoc.getProcStepAttrAssocId(), true);
            log.info("🔗 Found {} rules for attribute association ID {}", ruleAssocs.size(), assoc.getProcStepAttrAssocId());

            for (ProcStepAttrRuleAssoc ruleAssoc : ruleAssocs) {
                processRule(ruleAssoc, payloadString, assoc.getProcStepAttrAssocId(), context);
            }
        }

        List<Map<String, String>> passedRules =
                buildRuleInfoList(ruleStatuses, ruleExecutionTimes, "PASSED");
        List<Map<String, String>> failedRules =
                buildRuleInfoList(ruleStatuses, ruleExecutionTimes, "FAILED");

        Map<String, Object> executionSummary = new LinkedHashMap<>();
        executionSummary.put("totalRuleExecutionTime", totalExecutionTime.get() + " ms");
        executionSummary.put("totalRulesPassed", passedRules.size());
        executionSummary.put("totalRulesFailed", failedRules.size());
        executionSummary.put("totalRulesErrored", erroredRules.size());

        Map<String, Object> finalRuleInfo = new LinkedHashMap<>();
        finalRuleInfo.put("passedRules", passedRules);
        finalRuleInfo.put("failedRules", failedRules);
        finalRuleInfo.put("erroredRules", erroredRules);
        executionSummary.put("ruleInfo", finalRuleInfo);

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String executionSummaryJson =
                    objectMapper
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(executionSummary);
            if (!failedRules.isEmpty() || !erroredRules.isEmpty()) {
                log.info("Rule Execution Summary: {}", executionSummaryJson);
            }
        } catch (Exception e) {
            log.error("Error while converting execution summary to JSON", e);
        }
        return errorMessages;
    }

    private void processRule(
            ProcStepAttrRuleAssoc ruleAssoc,
            String payloadString,
            String assocId,
            RuleProcessingContext ctx) {
        Rule rule = ruleAssoc.getRule();
        try {
            Map<String, Object> ruleResult =
                    ruleExecutionService.evaluateWithExecutionTime(rule, payloadString);
            long executionTime = (long) ruleResult.get("executionTime");
            ctx.totalExecutionTime().addAndGet(executionTime);
            ctx.ruleExecutionTimes().put(rule.getRuleId(), executionTime + " ms");

            @SuppressWarnings("unchecked")
            List<String> ruleErrors = (List<String>) ruleResult.get("errors");
            if (!ruleErrors.isEmpty()) {
                ctx.ruleStatuses().put(rule.getRuleId(), "FAILED");
                ruleErrors.forEach(
                        msg -> {
                            Message runtimeMsg =
                                    Message.builder()
                                            .messageId(UUID.randomUUID().toString())
                                            .processStepOutcomeId(
                                                    ctx.stepOutcome().getProcessStepOutcomeId())
                                            .message(msg)
                                            .build();
                            messageRepository.save(runtimeMsg);
                            ruleValidationLogs(rule, assocId);
                            log.info(
                                    "X Saved failure message for rule [{}] : {}",
                                    rule.getRuleId(),
                                    msg);
                            ctx.errorMessages().add(msg);
                        });
            } else {
                ctx.ruleStatuses().put(rule.getRuleId(), "PASSED");
            }
        } catch (Exception e) {
            ruleValidationLogs(rule, assocId);
            ctx.ruleStatuses().put(rule.getRuleId(), "ERRORED");
            ctx.ruleExecutionTimes().put(rule.getRuleId(), "0 ms");

            String errorMsg = "Error evaluating rule " + rule.getRuleId() + ": " + e.getMessage();
            Message runtimeMsg =
                    Message.builder()
                            .messageId(UUID.randomUUID().toString())
                            .processStepOutcomeId(ctx.stepOutcome().getProcessStepOutcomeId())
                            .message(errorMsg)
                            .build();
            messageRepository.save(runtimeMsg);

            Map<String, String> errorRuleInfo = new HashMap<>();
            errorRuleInfo.put("ruleId", rule.getRuleId());
            errorRuleInfo.put("errorMessage", e.getMessage());
            ctx.erroredRules().add(errorRuleInfo);

            log.error("X Error evaluating rule [{}]: {}", rule.getRuleId(), e.getMessage());
            ctx.errorMessages()
                    .add("Error evaluating rule " + rule.getRuleId() + "; " + e.getMessage());
        }
    }

    private void ruleValidationLogs(Rule rule, String assocId) {
        log.info(
                "Validating Rule [{}] - '{}' linked to AttrAssoc [{}]",
                rule.getRuleId(),
                rule.getName(),
                assocId);
    }

    private List<Map<String, String>> buildRuleInfoList(
            Map<String, String> ruleStatuses,
            Map<String, String> ruleExecutionTimes,
            String desiredStatus) {
        return ruleStatuses.entrySet().stream()
                .filter(entry -> desiredStatus.equals(entry.getValue()))
                .map(
                        entry -> {
                            Map<String, String> ruleInfo = new HashMap<>();
                            ruleInfo.put("ruleId", entry.getKey());
                            ruleInfo.put("executionTime", ruleExecutionTimes.get(entry.getKey()));
                            return ruleInfo;
                        })
                .toList();
    }

    private String determineNextStepId(ProcessStep currentStep, List<String> errorMessages) {
        int nextOrder = currentStep.getStepOrder() + 1;
        Optional<ProcessStep> nextStepOptional =
                processStepRepository.findNextStep(
                        currentStep.getProcessEntity().getProcessId(), nextOrder);

        if (nextStepOptional.isPresent()) {
            log.info(
                    "Found next step: {} with order {}",
                    nextStepOptional.get().getProcessStepId(),
                    nextOrder);
            return nextStepOptional.get().getProcessStepId();
        }

        if (!errorMessages.isEmpty()) {
            log.info("Errors detected, using error step");
            return "some_fix_error_next_step_id";
        }

        log.info(
                "No next step found for process {} after step order {}. End of process.",
                currentStep.getProcessEntity().getProcessId(),
                currentStep.getStepOrder());
        return "process_completed";
    }

    private EvaluateStepResponse buildResponse(
            ProcessStepOutcome stepOutcome, List<String> messages) {
        return EvaluateStepResponse.builder()
                .correlationId(stepOutcome.getCorrelationId())
                .transmissionId(stepOutcome.getTransmissionId())
                .status(stepOutcome.getStatus())
                .messages(messages)
                .nextStep(stepOutcome.getNextStepId())
                .build();
    }
}

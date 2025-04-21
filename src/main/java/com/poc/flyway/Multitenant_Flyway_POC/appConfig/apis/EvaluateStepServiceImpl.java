package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.IEvaluateStepService;
import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.client.ActionContext;
import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.MessagesRepository;
import com.adp.benefits.carrier.entity.client.repository.ProcessStepOutcomeRepository;
import com.adp.benefits.carrier.entity.primary.ProcStepAttrAssoc;
import com.adp.benefits.carrier.entity.primary.ProcStepAttrRuleAssoc;
import com.adp.benefits.carrier.entity.primary.ProcessStep;
import com.adp.benefits.carrier.entity.primary.Rule;
import com.adp.benefits.carrier.entity.primary.repository.ProcStepAttrAssocRepository;
import com.adp.benefits.carrier.entity.primary.repository.ProcStepAttrRuleAssocRepository;
import com.adp.benefits.carrier.entity.primary.repository.ProcessStepRepository;
import com.adp.benefits.carrier.enums.Status;
import com.adp.benefits.carrier.exceptions.ClientValidationException;
import com.adp.benefits.carrier.exceptions.ProcessStepValidationException;
import com.adp.benefits.carrier.model.EvaluateStepRequest;
import com.adp.benefits.carrier.model.EvaluateStepResponse;
import com.adp.benefits.carrier.model.ruleDtos.RuleError;
import com.adp.benefits.carrier.model.ruleDtos.RuleExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
@RequiredArgsConstructor
public class EvaluateStepServiceImpl implements IEvaluateStepService {

    private final ProcessStepOutcomeRepository outcomeRepo;
    private final ProcessStepRepository stepRepo;
    private final IRuleExecutionService ruleSvc;
    private final ProcStepAttrAssocRepository assocRepo;
    private final ProcStepAttrRuleAssocRepository ruleAssocRepo;
    private final MessagesRepository messageRepo;
    private final ObjectMapper mapper;

    @Transactional
    @Override
    public EvaluateStepResponse evaluateStep(EvaluateStepRequest request)
            throws JsonProcessingException {
        log.info("▶️ Evaluating step for correlationId: {}", request.getCorrelationId());

        // 1) validate
        try {
            validateRequest(request);
        } catch (ClientValidationException e) {
            log.error("Client validation error: {}", e.getMessage());
            return buildErrorResponse(request, e.getMessage());
        }

        // 2) load or create our ProcessStepOutcome
        ProcessStep step = loadProcessStep(request.getProcessStepId());
        ProcessStepOutcome outcome = findOrCreateOutcome(request, step);

        // 3) skip logic
        if (isSkipped(request.getClientId(), step.getProcessStepId())) {
            markSkipped(outcome);
            return buildResponse(outcome, Collections.emptyList());
        }

        // 4) store incoming payload
        storeActionContext(outcome, request.getPayload());

        // 5) run rules
        RuleExecutionSummary summary = new RuleExecutionSummary();
        List<RuleError> allErrors = new ArrayList<>();
        String payloadJson = request.getPayload().toString();

        var assocs = assocRepo.findByProcessStepIdAndEnabledIn(step.getProcessStepId(), true);
        log.info(
                "🧩 Found {} attribute associations for stepId {}",
                assocs.size(),
                step.getProcessStepId());

        for (ProcStepAttrAssoc assoc : assocs) {
            var ruleAssocs =
                    ruleAssocRepo.findByProcStepAttrAssocIdAndEnabledIn(
                            assoc.getProcStepAttrAssocId(), true);
            log.info(
                    "🔗 Found {} rules for AttrAssoc {}",
                    ruleAssocs.size(),
                    assoc.getProcStepAttrAssocId());

            for (ProcStepAttrRuleAssoc ra : ruleAssocs) {
                Rule rule = ra.getRule();

                // Typed call returns a rich DTO
                RuleExecutionResult result =
                        ruleSvc.evaluateWithExecutionTime(
                                rule, payloadJson, assoc.getAttribute().getAttributeId());

                // record for summary
                summary.record(
                        result.getRuleId(),
                        result.getExecutionTimeMs(),
                        result.getErrors().isEmpty());

                // persist each RuleError
                for (RuleError err : result.getErrors()) {
                    allErrors.add(err);
                    messageRepo.save(
                            Message.builder()
                                    .messageId(UUID.randomUUID().toString())
                                    .processStepOutcomeId(outcome.getProcessStepOutcomeId())
                                    .message(mapper.writeValueAsString(err))
                                    .build());
                }
            }
        }

        // 6) log summary
        logSummary(summary);

        // 7) finalize outcome
        outcome.setStatus(allErrors.isEmpty() ? Status.CONTINUE : Status.ERROR);
        outcome.setNextStepId(determineNextStepId(step, allErrors));
        outcome.setUpdatedAt(new Date());
        outcomeRepo.save(outcome);

        // 8) return DTO‑based response
        return buildResponse(outcome, allErrors);
    }

    // ────────────────────────────────────────────────────────────

    private void validateRequest(EvaluateStepRequest r) {
        Objects.requireNonNull(r.getCorrelationId(), "correlationId is required");
        Objects.requireNonNull(r.getTransmissionId(), "transmissionId is required");
        Objects.requireNonNull(r.getClientId(), "clientId is required");
        Objects.requireNonNull(r.getProcessId(), "processId is required");
        Objects.requireNonNull(r.getProcessStepId(), "processStepId is required");
        Objects.requireNonNull(r.getSystemId(), "systemId is required");
        if (r.getPayload() == null || r.getPayload().isNull() || r.getPayload().isEmpty()) {
            throw new ClientValidationException("Payload is required.");
        }

        ProcessStep ps =
                stepRepo.findById(r.getProcessStepId())
                        .orElseThrow(
                                () ->
                                        new ProcessStepValidationException(
                                                "ProcessStep not found: " + r.getProcessStepId()));

        if (!r.getProcessId().equals(ps.getProcessEntity().getProcessId())) {
            throw new ProcessStepValidationException(
                    "ProcessStep does not belong to process " + r.getProcessId());
        }
        if (!r.getSystemId().equals(ps.getProcessEntity().getSystemEntity().getSystemId())) {
            throw new ProcessStepValidationException(
                    "ProcessStep does not belong to system " + r.getSystemId());
        }
    }

    private ProcessStep loadProcessStep(String id) {
        return stepRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessStep not found: " + id));
    }

    private ProcessStepOutcome findOrCreateOutcome(EvaluateStepRequest req, ProcessStep step) {
        return outcomeRepo
                .findByCorrelationIdAndTransmissionIdAndProcessStepId(
                        req.getCorrelationId(), req.getTransmissionId(), step.getProcessStepId())
                .orElseGet(
                        () -> {
                            ProcessStepOutcome o =
                                    ProcessStepOutcome.builder()
                                            .processStepOutcomeId(UUID.randomUUID().toString())
                                            .correlationId(req.getCorrelationId())
                                            .transmissionId(req.getTransmissionId())
                                            .clientId(req.getClientId())
                                            .processStepId(step.getProcessStepId())
                                            .status(Status.IN_PROGRESS)
                                            .nextStepId("")
                                            .createdAt(new Date())
                                            .updatedAt(new Date())
                                            .build();
                            return outcomeRepo.save(o);
                        });
    }

    private boolean isSkipped(String clientId, String stepId) {
        return "overrideClient".equals(clientId) && "overrideStep".equals(stepId);
    }

    private void markSkipped(ProcessStepOutcome o) {
        o.setStatus(Status.SKIPPED);
        o.setNextStepId("");
        o.setUpdatedAt(new Date());
        outcomeRepo.save(o);
    }

    private void storeActionContext(ProcessStepOutcome o, JsonNode payload) {
        String tx = o.getTransmissionId();
        o.getActionContexts().stream()
                .filter(ac -> tx.equals(ac.getTransmissionId()))
                .findFirst()
                .ifPresentOrElse(
                        ac -> ac.setPayload(payload),
                        () ->
                                o.getActionContexts()
                                        .add(
                                                ActionContext.builder()
                                                        .transmissionId(tx)
                                                        .processStepOutcomeId(
                                                                o.getProcessStepOutcomeId())
                                                        .payload(payload)
                                                        .build()));
    }

    private String determineNextStepId(ProcessStep current, List<RuleError> failures) {
        int nextOrder = current.getStepOrder() + 1;
        return stepRepo.findNextStep(current.getProcessEntity().getProcessId(), nextOrder)
                .map(ProcessStep::getProcessStepId)
                .orElseGet(
                        () -> {
                            if (!failures.isEmpty()) {
                                log.info("Errors detected, routing to error step");
                                return "some_fix_error_next_step_id";
                            }
                            log.info(
                                    "End of process for {}",
                                    current.getProcessEntity().getProcessId());
                            return "process_completed";
                        });
    }

    private EvaluateStepResponse buildErrorResponse(EvaluateStepRequest req, String message) {
        RuleError err =
                RuleError.builder()
                        .ruleId(null)
                        .message(message)
                        .errorDetails(Collections.emptyList())
                        .build();
        return EvaluateStepResponse.builder()
                .correlationId(req.getCorrelationId())
                .transmissionId(req.getTransmissionId())
                .status(Status.ERROR)
                .messages(List.of(err))
                .nextStep("")
                .build();
    }

    private EvaluateStepResponse buildResponse(
            ProcessStepOutcome outcome, List<RuleError> messages) {
        return EvaluateStepResponse.builder()
                .correlationId(outcome.getCorrelationId())
                .transmissionId(outcome.getTransmissionId())
                .status(outcome.getStatus())
                .messages(messages)
                .nextStep(outcome.getNextStepId())
                .build();
    }

    private void logSummary(RuleExecutionSummary summary) {
        try {
            String json =
                    mapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary.toMap());
            log.info("Rule Execution Summary: {}", json);
        } catch (Exception e) {
            log.error("Failed to serialize RuleExecutionSummary", e);
        }
    }

    /** Tracks per‐rule execution time & pass/fail. */
    private static class RuleExecutionSummary {
        private final AtomicLong totalTime = new AtomicLong();
        private final Map<String, Long> executionTimes = new LinkedHashMap<>();
        private final Map<String, String> statuses = new LinkedHashMap<>();

        void record(String ruleId, long ms, boolean passed) {
            totalTime.addAndGet(ms);
            executionTimes.put(ruleId, ms);
            statuses.put(ruleId, passed ? "PASSED" : "FAILED");
        }

        Map<String, Object> toMap() {
            long passed = statuses.values().stream().filter("PASSED"::equals).count();
            long failed = statuses.size() - passed;

            var out = new LinkedHashMap<String, Object>();
            out.put("totalRuleExecutionTime", totalTime.get() + " ms");
            out.put("totalRulesPassed", passed);
            out.put("totalRulesFailed", failed);
            out.put("totalRulesErrored", 0);

            var detail = new LinkedHashMap<String, Object>();
            detail.put("passedRules", buildRuleInfo("PASSED"));
            detail.put("failedRules", buildRuleInfo("FAILED"));
            detail.put("erroredRules", Collections.emptyList());
            out.put("ruleInfo", detail);

            return out;
        }

        private List<Map<String, String>> buildRuleInfo(String filter) {
            return statuses.entrySet().stream()
                    .filter(e -> filter.equals(e.getValue()))
                    .map(
                            e ->
                                    Map.of(
                                            "ruleId",
                                            e.getKey(),
                                            "executionTime",
                                            executionTimes.get(e.getKey()) + " ms"))
                    .toList();
        }
    }
}

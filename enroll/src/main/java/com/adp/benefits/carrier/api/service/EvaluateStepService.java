package com.adp.benefits.carrier.api.service;

import com.adp.benefits.carrier.api.service.interfaces.IRuleExecutionService;
import com.adp.benefits.carrier.api.service.mapper.CanonicalEnrollmentInfoMapper;
import com.adp.benefits.carrier.api.service.mapper.MapperUtil;
import com.adp.benefits.carrier.entity.client.*;
import com.adp.benefits.carrier.entity.client.repository.*;
import com.adp.benefits.carrier.entity.primary.*;
import com.adp.benefits.carrier.entity.primary.repository.ProcStepAttrAssocRepository;
import com.adp.benefits.carrier.entity.primary.repository.ProcStepAttrRuleAssocRepository;
import com.adp.benefits.carrier.entity.primary.repository.ProcessStepRepository;
import com.adp.benefits.carrier.exceptions.ClientValidationException;
import com.adp.benefits.carrier.exceptions.ProcessStepValidationException;
import com.adp.benefits.carrier.model.RuleExecutionResult;
import com.adp.benefits.carrier.model.cpm.enums.State;
import com.adp.benefits.carrier.model.cpm.enums.Status;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepRequest;
import com.adp.benefits.carrier.model.cpm.models.EvaluateStepResponse;
import com.adp.benefits.carrier.model.cpm.models.RuleError;
import com.adp.benefits.carrier.service.ProcessStepOutcomeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluateStepService {

    private final ProcessStepOutcomeService processStepOutcomeService;
    private final ProcessStepRepository processStepRepository;
    private final IRuleExecutionService ruLeSvc;
    private final ProcStepAttrAssocRepository procStepAttrAssocRepository;
    private final ProcStepAttrRuleAssocRepository procStepAttrRuleAssocRepository;
    private final MessagesRepository messagesRepository;
    private final EnrollmentInfoRepository enrollmentInfoRepository;
    private final PlanTypeRepository planTypeRepository;
    private final ParticipantRepository participantRepository;
    private final PlanTypeEnrollInfoAssocRepository planTypeEnrollInfoAssocRepository;
    private final ObjectMapper mapper;

    private static final String BENEFIT_COVERAGES_PATH = "benefitCoverages";
    private static final String COVERAGE_TYPE_CODE_PATH = "coverageTypeCode";
    private static final String CODE_PATH = "code";

    private static final String STATUS_PASSED = "PASSED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_ERRORED = "ERRORED";

    // used by outcome & next-step helpers in the current screenshots
    private static final String OVERRIDE_CLIENT = "OVERRIDE_CLIENT";
    private static final String OVERRIDE_STEP = "OVERRIDE_STEP";
    private static final String ERROR_NEXT_STEP_ID = "SOME_FIX_ERROR_NEXT_STEP_ID";
    private static final String PROCESS_COMPLETED = "PROCESS_COMPLETED";
    private static final String SYSTEM_ERROR = "SYSTEM_ERROR";

    @Transactional
    public EvaluateStepResponse evaluate(EvaluateStepRequest request)
            throws JsonProcessingException {
        log.info("🟩 Evaluating step for correlationId: {}", request.getCorrelationId());

        try {
            validateRequest(request);
        } catch (ClientValidationException e) {
            log.error("Client validation error: {}", e.getMessage());
            return buildErrorResponse(request, e.getMessage());
        }

        ProcessStep step = loadProcessStep(request.getProcessStepId());
        ProcessStepOutcome outcome =
                processStepOutcomeService.findOrCreateOutcome(
                        request.getCorrelationId().toString(),
                        request.getTransmissionId().toString(),
                        request.getClientId(),
                        request.getProcessStepId(),
                        request.getAoid());

        if (isSkipped(request.getClientId(), step.getProcessStepId())) {
            markSkipped(outcome);
            return buildResponse(outcome, Collections.emptyList());
        }

        storeActionContext(outcome, request.getPayload());

        RuleExecutionSummary summary = new RuleExecutionSummary();
        List<RuleError> allErrors = new ArrayList<>();
        String payloadJson = request.getPayload().toString();

        var assocs =
                procStepAttrAssocRepository.findByProcessStepIdAndEnabledIn(
                        step.getProcessStepId(), true);
        log.info(
                "• Found {} attribute associations for stepId {}",
                assocs.size(),
                step.getProcessStepId());

        processRules(assocs, payloadJson, summary, allErrors, outcome);
        logSummary(summary);

        determineOutcomeState(step, outcome, allErrors);

        outcome.setNextStepId(determineNextStepId(step, allErrors));
        processStepOutcomeService.saveProcessStepOutcome(outcome);

        persistEnrollmentInfo(outcome, request.getPayload());
        persistParticipants(outcome, request.getPayload());

        return buildResponse(outcome, allErrors);
    }

    /* ===== processing ===== */

    private void processRules(
            List<ProcStepAttrAssoc> procStepAttrAssocs,
            String payloadJson,
            RuleExecutionSummary summary,
            List<RuleError> allErrors,
            ProcessStepOutcome outcome)
            throws JsonProcessingException {
        for (ProcStepAttrAssoc assoc : procStepAttrAssocs) {
            processAssoc(assoc, payloadJson, summary, allErrors, outcome);
        }
    }

    private void processAssoc(
            ProcStepAttrAssoc procStepAttrAssoc,
            String payloadJson,
            RuleExecutionSummary summary,
            List<RuleError> allErrors,
            ProcessStepOutcome outcome)
            throws JsonProcessingException {
        var ruleAssocs =
                procStepAttrRuleAssocRepository.findByProcStepAttrAssocIdAndEnabledIn(
                        procStepAttrAssoc.getProcStepAttrAssocId(), true);

        log.info(
                "• Found {} rules for AttrAssoc {}",
                ruleAssocs.size(),
                procStepAttrAssoc.getProcStepAttrAssocId());

        for (ProcStepAttrRuleAssoc ruleAssoc : ruleAssocs) {
            processRule(
                    ruleAssoc.getRule(),
                    payloadJson,
                    procStepAttrAssoc.getAttribute().getAttributeId(),
                    summary,
                    allErrors,
                    outcome);
        }
    }

    private void processRule(
            Rule rule,
            String payloadJson,
            String attributeId,
            RuleExecutionSummary summary,
            List<RuleError> allErrors,
            ProcessStepOutcome outcome)
            throws JsonProcessingException {
        RuleExecutionResult result =
                ruLeSvc.evaluateWithExecutionTime(rule, payloadJson, attributeId);

        String status = determineRuleStatus(result);
        summary.record(result.getRuleId(), result.getExecutionTimeMs(), status);
        persistErrors(result.getErrors(), allErrors, outcome);
    }

    private String determineRuleStatus(RuleExecutionResult result) {
        if (result.getErrors().isEmpty()) {
            return STATUS_PASSED;
        } else if (result.isErrored()) {
            return STATUS_ERRORED;
        } else {
            return STATUS_FAILED;
        }
    }

    private void persistErrors(
            List<RuleError> errors, List<RuleError> allErrors, ProcessStepOutcome outcome) {
        for (RuleError err : errors) {
            allErrors.add(err);
            messagesRepository.save(
                    Message.builder()
                            .messageId(UUID.randomUUID().toString())
                            .processStepOutcomeId(outcome.getProcessStepOutcomeId())
                            .message(mapper.valueToTree(err))
                            .createDt(Date.from(Instant.now()))
                            .modifyDt(Date.from(Instant.now()))
                            .build());
        }
    }

    /* ===== validation & lookups ===== */

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
                processStepRepository
                        .findById(r.getProcessStepId())
                        .orElseThrow(
                                () ->
                                        new ProcessStepValidationException(
                                                "processStep not found: " + r.getProcessStepId()));

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
        return processStepRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("ProcessStep not found: " + id));
    }

    /* ===== skip + context ===== */

    // TODO: SKIP LOGIC NOT FINALIZED
    private boolean isSkipped(String clientId, String stepId) {
        return OVERRIDE_CLIENT.equals(clientId) && OVERRIDE_STEP.equals(stepId);
    }

    // TODO: SKIP LOGIC NOT FINALIZED
    private void markSkipped(ProcessStepOutcome o) {
        o.setStatus(Status.SKIPPED);
        o.setState(State.SUCCESS); // Assuming skipped is a success state
        o.setModifyDt(Date.from(Instant.now()));
        processStepOutcomeService.saveProcessStepOutcome(o);
    }

    private void storeActionContext(ProcessStepOutcome processStepOutcome, JsonNode payload) {
        String tx = processStepOutcome.getTransmissionId();
        processStepOutcome.getActionContexts().stream()
                .filter(ac -> tx.equals(ac.getTransmissionId()))
                .findFirst()
                .ifPresentOrElse(
                        ac -> ac.setPayload(payload),
                        () ->
                                processStepOutcome
                                        .getActionContexts()
                                        .add(
                                                ActionContext.builder()
                                                        .transmissionId(tx)
                                                        .processStepOutcomeId(
                                                                processStepOutcome
                                                                        .getProcessStepOutcomeId())
                                                        .payload(payload)
                                                        .createDt(Date.from(Instant.now()))
                                                        .build()));
    }

    /* ===== next-step & responses ===== */

    private String determineNextStepId(ProcessStep current, List<RuleError> failures) {
        int nextOrder = current.getStepOrder() + 1;
        return processStepRepository
                .findNextStep(current.getProcessEntity().getProcessId(), nextOrder)
                .map(ProcessStep::getProcessStepId)
                .orElseGet(
                        () -> {
                            if (!failures.isEmpty()) {
                                log.info("Errors detected, routing to error step");
                                return ERROR_NEXT_STEP_ID;
                            }
                            log.info(
                                    "End of process for {}",
                                    current.getProcessEntity().getProcessId());
                            return PROCESS_COMPLETED;
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
                .status(Status.STOP)
                .state(State.VALIDATION_ERROR)
                .messages(List.of(err))
                .nextStep("")
                .build();
    }

    private EvaluateStepResponse buildResponse(
            ProcessStepOutcome outcome, List<RuleError> messages) {
        UUID correlationId =
                outcome.getCorrelationId() != null && !outcome.getCorrelationId().isEmpty()
                        ? UUID.fromString(outcome.getCorrelationId())
                        : null;

        UUID transmissionId =
                outcome.getTransmissionId() != null && !outcome.getTransmissionId().isEmpty()
                        ? UUID.fromString(outcome.getTransmissionId())
                        : null;

        return EvaluateStepResponse.builder()
                .correlationId(correlationId)
                .transmissionId(transmissionId)
                .status(outcome.getStatus())
                .state(outcome.getState())
                .messages(messages)
                .nextStep(outcome.getNextStepId())
                .build();
    }

    private void logSummary(RuleExecutionSummary summary) {
        try {
            String json = mapper.writeValueAsString(summary.toMap());
            log.info("Rule Execution Summary: {}", json);
        } catch (Exception e) {
            log.error("Failed to serialize RuleExecutionSummary", e);
        }
    }

    /* ===== summary ===== */

    private static final class RuleExecutionSummary {
        private final AtomicLong totalTime = new AtomicLong();
        private final Map<String, Long> executionTimes = new LinkedHashMap<>();
        private final Map<String, String> statuses = new LinkedHashMap<>();

        void record(String ruleId, long ms, String status) {
            totalTime.addAndGet(ms);
            executionTimes.put(ruleId, ms);
            statuses.put(ruleId, status);
        }

        Map<String, Object> toMap() {
            long passed = statuses.values().stream().filter(STATUS_PASSED::equals).count();
            long failed = statuses.values().stream().filter(STATUS_FAILED::equals).count();
            long errored = statuses.values().stream().filter(STATUS_ERRORED::equals).count();

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("totalRuleExecutionTime", totalTime.get() + " ms");
            out.put("totalRulesPassed", passed);
            out.put("totalRulesFailed", failed);
            out.put("totalRulesErrored", errored);

            Map<String, Object> details = new LinkedHashMap<>();
            details.put("passedRules", buildRuleInfo(STATUS_PASSED));
            details.put("failedRules", buildRuleInfo(STATUS_FAILED));
            details.put("erroredRules", buildRuleInfo(STATUS_ERRORED));

            out.put("ruleInfo", details);
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

    /**
     * Determines the appropriate State for the ProcessStepOutcome based on rule execution results
     */
    private void determineOutcomeState(
            ProcessStep processStep, ProcessStepOutcome outcome, List<RuleError> errors) {
        boolean hasWarnings = errors.stream().anyMatch(RuleError::isWarning);

        if (errors.isEmpty()) {
            if (!processStep.isAsyncIn()) {
                outcome.setStatus(Status.CONTINUE);
                outcome.setState(State.SUCCESS);
            } else {
                outcome.setStatus(Status.ASYNC);
                outcome.setState(State.ONE_CG_SEND);
                log.info(
                        "Setting ONE_CG_SEND state for async step: {}",
                        outcome.getProcessStepOutcomeId());
            }
            return;
        }

        outcome.setStatus(hasWarnings ? Status.WARNING : Status.STOP);

        if (shouldSetPauseState(outcome.getClientId(), outcome.getProcessStepId())) {
            outcome.setState(State.PAUSE);
            log.info("Setting PAUSE state for outcome: {}", outcome.getProcessStepOutcomeId());
            return;
        }

        if (shouldSetVolumeErrorState(errors)) {
            outcome.setState(State.VOLUME_ERROR);
            log.info(
                    "Setting VOLUME_ERROR state for outcome: {}",
                    outcome.getProcessStepOutcomeId());
            return;
        }

        // Determine if there are critical errors (system errors) or validation errors
        boolean hasCriticalError =
                errors.stream()
                        .anyMatch(
                                err ->
                                        err.getRuleId() == null
                                                || STATUS_ERRORED.equals(getErrorStatus(err)));

        if (hasCriticalError) {
            outcome.setState(State.CRITICAL_ERROR);
            log.info(
                    "Setting CRITICAL_ERROR state for outcome: {}",
                    outcome.getProcessStepOutcomeId());
        } else {
            outcome.setState(State.VALIDATION_ERROR);
            log.info(
                    "Setting VALIDATION_ERROR state for outcome: {}",
                    outcome.getProcessStepOutcomeId());
        }
    }

    /**
     * Helper method to determine if PAUSE state should be set Implementation placeholder - will be
     * implemented once requirements are finalized
     */
    private boolean shouldSetPauseState(String clientId, String processStepId) {
        // TODO: Implement pause state determination logic once requirements are finalized
        log.debug(
                "Checking pause state configuration for client: {} and step: {}",
                clientId,
                processStepId);
        return false; // Default implementation returns false until requirements are finalized
    }

    /**
     * Helper method to determine if VOLUME_ERROR state should be set Implementation placeholder -
     * will be implemented once requirements are finalized
     */
    private boolean shouldSetVolumeErrorState(List<RuleError> errors) {
        // TODO: Implement volume error detection logic once requirements are finalized
        log.debug("Checking for volume errors in {} rule errors", errors.size());
        return false; // Default implementation returns false until requirements are finalized
    }

    /** Helper method to determine the error status from a RuleError */
    private String getErrorStatus(RuleError error) {
        // If error was thrown during rule execution (not a validation failure)
        if (error.getRuleId() == null) {
            return STATUS_ERRORED;
        }

        // Since we don't have access to error type, we'll rely on the error message
        boolean hasSystemError =
                error.getErrorDetails().stream()
                        .anyMatch(detail -> detail.toString().contains(SYSTEM_ERROR));

        return hasSystemError ? STATUS_ERRORED : STATUS_FAILED;
    }

    /** Persists enrollment information and associated plan types from the JSON payload. */
    public void persistEnrollmentInfo(ProcessStepOutcome outcome, JsonNode payload) {
        Set<String> carrierId = new HashSet<>();
        List<String> coverageTypeCodes = new ArrayList<>();
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

            SystemEntity systemEntity =
                    MapperUtil.mapProcessStepIdToSystemEntity(
                            outcome.getProcessStepId(), processStepRepository);

            if (systemEntity.getSystemId().equalsIgnoreCase("CGR")) {
                CanonicalEnrollmentInfoMapper mapper = new CanonicalEnrollmentInfoMapper();
                carrierId = mapper.extractCarrierIds(payload);
                coverageTypeCodes = mapper.extractBenefitCoverages(payload);
            }

            if (carrierId == null || coverageTypeCodes.isEmpty()) {
                log.debug("No carrier ID or coverage type codes found in payload");
                return;
            }

            EnrollmentInfo enrollInfo =
                    EnrollmentInfo.builder()
                            .enrollInfoId(UUID.randomUUID().toString())
                            .processStepOutcome(outcome)
                            .carrierId(String.join(", ", carrierId))
                            .planTypeEnrollInfoAssocs(new HashSet<>())
                            .build();

            // Save the new enrollment info
            enrollmentInfoRepository.save(enrollInfo);

            // Loop over each coverage type for multiple plan types
            for (String coverageTypeCode : coverageTypeCodes) {
                PlanType planType =
                        PlanType.builder()
                                .planTypeId(UUID.randomUUID().toString())
                                .planName(coverageTypeCode)
                                .planTypeEnrollInfoAssocs(new ArrayList<>())
                                .build();
                planTypeRepository.save(planType);

                // Create association between enrollment info and plan type
                PlanTypeEnrollInfoAssoc association =
                        PlanTypeEnrollInfoAssoc.builder()
                                .planTypeId(planType.getPlanTypeId())
                                .enrollInfoId(enrollInfo.getEnrollInfoId())
                                .planType(planType)
                                .enrollmentInfo(enrollInfo)
                                .build();

                enrollInfo.getPlanTypeEnrollInfoAssocs().add(association);
                planType.getPlanTypeEnrollInfoAssocs().add(association);

                // Explicitly persist the association
                planTypeEnrollInfoAssocRepository.save(association);
            }

            // Save the updated enrollment info
            enrollmentInfoRepository.save(enrollInfo);

            log.info(
                    "✅ Successfully persisted enrollment info with ID: {} and associated plan types",
                    enrollInfo.getEnrollInfoId());
        } catch (Exception e) {
            log.error(
                    "❌ Error persisting enrollment info for processStepOutcomeId: {}",
                    outcome.getProcessStepOutcomeId(),
                    e);
            throw e;
        }
    }

    /** Persists participant information from the JSON payload. */
    private void persistParticipants(ProcessStepOutcome outcome, JsonNode payload) {
        try {
            log.debug(
                    "✳️ Persisting participant info for processStepOutcomeId: {}",
                    outcome.getProcessStepOutcomeId());

            if (payload == null) {
                log.debug("No payload provided for participant persistence");
                return;
            }

            JsonNode workerNode = payload.path("worker");
            if (workerNode.isMissingNode()) {
                log.debug("No worker information found in payload");
                return;
            }

            JsonNode personNode = workerNode.path("person");
            if (personNode.isMissingNode()) {
                log.debug("No person information found in payload");
                return;
            }

            String firstName = extractFirstName(personNode);
            String lastName = extractLastName(personNode);
            String ssn = extractSSN(personNode);
            String jobTitle = extractJobTitle(workerNode);

            Participant participant =
                    Participant.builder()
                            .participantId(UUID.randomUUID().toString())
                            .firstName(firstName)
                            .lastName(lastName)
                            .ssn(ssn)
                            .jobId(null) // TODO: null for now as specified
                            .participantType(null) // TODO: null for now as specified
                            .jobTitle(jobTitle)
                            .clientId(outcome.getClientId())
                            .aoid(outcome.getAoid())
                            .build();

            participantRepository.save(participant);

            log.info(
                    "✅ Successfully persisted participant with ID: {}",
                    participant.getParticipantId());
        } catch (Exception e) {
            log.error(
                    "❌ Error persisting participant for processStepOutcomeId: {}",
                    outcome.getProcessStepOutcomeId(),
                    e);
            // Not rethrowing the exception to allow the process to continue
            // This is consistent with how persistEnrollmentInfo handles exceptions in your
            // screenshots
        }
    }

    /** Extracts the first name from person node. */
    private String extractFirstName(JsonNode personNode) {
        return personNode.path("legalName").path("givenName").asText(null);
    }

    /** Extracts the last name from person node. */
    private String extractLastName(JsonNode personNode) {
        return personNode.path("legalName").path("familyName").asText(null);
    }

    /** Extracts SSN from government IDs if available. */
    private String extractSSN(JsonNode personNode) {
        JsonNode governmentIDs = personNode.path("governmentIDs");
        if (governmentIDs.isArray()) {
            for (JsonNode idNode : governmentIDs) {
                JsonNode nameCodeNode = idNode.path("nameCode");
                if (!nameCodeNode.isMissingNode()
                        && "SSN".equals(nameCodeNode.path("code").asText())) {
                    return idNode.path("id").asText(null);
                }
            }
        }
        return null;
    }

    /** Extracts job title from work assignments if available. */
    private String extractJobTitle(JsonNode workerNode) {
        JsonNode workAssignments = workerNode.path("workAssignments");
        if (workAssignments.isArray() && !workAssignments.isEmpty()) {
            JsonNode jobNode = workAssignments.path(0).path("job");
            if (!jobNode.isMissingNode() && jobNode.has("jobTitle")) {
                return jobNode.get("jobTitle").asText();
            }
        }
        return null;
    }
}

package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.IEvaluateStepService;
import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.client.ActionContext;
import com.adp.benefits.carrier.entity.client.Message;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import com.adp.benefits.carrier.entity.client.repository.ActionContextRepository;
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
    private final ActionContextRepository actionContextRepository;
    private final AttributeRepository attributeRepository;
    private final RuleRepository ruleRepository;
    private final IRuleExecutionService ruleExecutionService;
    private final ClientStepOverrideRepository clientStepOverrideRepository;
    private final ProcStepAttrAssocRepository procStepAttrAssocRepository;
    private final ProcStepAttrRuleAssocRepository procStepAttrRuleAssocRepository;
    private final MessagesRepository messageRepository;

    public EvaluateStepServiceImpl(
            ProcessStepOutcomeRepository processStepOutcomeRepository,
            ProcessRepository processRepository,
            ProcessStepRepository processStepRepository,
            ActionContextRepository actionContextRepository,
            AttributeRepository attributeRepository,
            RuleRepository ruleRepository,
            IRuleExecutionService ruleExecutionService,
            ClientStepOverrideRepository clientStepOverrideRepository,
            ProcStepAttrAssocRepository procStepAttrAssocRepository,
            ProcStepAttrRuleAssocRepository procStepAttrRuleAssocRepository,
            MessagesRepository messageRepository
    ) {
        this.processStepOutcomeRepository = processStepOutcomeRepository;
        this.processRepository = processRepository;
        this.processStepRepository = processStepRepository;
        this.actionContextRepository = actionContextRepository;
        this.attributeRepository = attributeRepository;
        this.ruleRepository = ruleRepository;
        this.ruleExecutionService = ruleExecutionService;
        this.clientStepOverrideRepository = clientStepOverrideRepository;
        this.procStepAttrAssocRepository = procStepAttrAssocRepository;
        this.procStepAttrRuleAssocRepository = procStepAttrRuleAssocRepository;
        this.messageRepository = messageRepository;
    }

    @Transactional
    @Override
    public EvaluateStepResponse evaluateStep(EvaluateStepRequest request) {
        log.info("Start evaluating step for correlationId: {}", request.getCorrelationId());

        // 1. Validate the request
        validateRequest(request);
        log.info("Request validated successfully.");

        // 2. Find the Process and Step
        ProcessEntity processEntity = findProcess(request.getProcessId());
        log.info("Process {} found.", processEntity.getProcessId());
        ProcessStep processStep = findProcessStep(request.getProcessStepId(), processEntity);
        log.info("ProcessStep {} found.", processStep.getProcessStepId());

        // 3. Find or Create ProcessStepOutcome
        ProcessStepOutcome stepOutcome = findOrCreateProcessStepOutcome(request, processStep);
        log.info("ProcessStepOutcome {} retrieved/created with status: {}.", stepOutcome.getProcessStepOutcomeId(), stepOutcome.getStatus());

        // 4. Check for Client Step Override (skip, pause, etc.)
        boolean skipStep = checkClientStepOverride(request.getClientId(), processStep.getProcessStepId());
        if (skipStep) {
            log.info("Client step override indicates skipping the step.");
            finalizeSkippedStep(stepOutcome, processStep);
            return buildResponse(stepOutcome, Collections.emptyList());
        }

        // 5. Store ActionContext (payload)
        storeActionContext(stepOutcome, request.getPayload());
        log.info("ActionContext stored for ProcessStepOutcome {}.", stepOutcome.getProcessStepOutcomeId());
        log.info("ActionContext: {}", stepOutcome.getActionContexts());

        // 6. Run Validations (Rules)
        List<String> errorMessages = runAttributeRules(processStep, request.getPayload(), stepOutcome);
        log.info("Rule evaluation completed. Errors found: {}.", errorMessages.size());

        // 7. Update status based on errors
        if (errorMessages.isEmpty()) {
            stepOutcome.setStatus(Status.CONTINUE);
            log.info("All rules passed. Setting status to COMPLETED.");
        } else {
            stepOutcome.setStatus(Status.ERROR);
            log.warn("Rule failures encountered. Setting status to ERROR.");
        }

        // 8. Determine next step (if any) and ensure it's non-null
        String nextStepId = determineNextStepId(stepOutcome, processStep, errorMessages);
        // Make sure nextStepId is non-null; use an empty string if there's no next step.
        if (nextStepId == null) {
            nextStepId = "";
            log.info("No valid next step determined; defaulting to empty string.");
        }
        stepOutcome.setNextStepId(nextStepId);
        log.info("Next step set to: {}", nextStepId);

        // 9. Save outcome with updated timestamp
        stepOutcome.setUpdatedAt(new Date());
        processStepOutcomeRepository.save(stepOutcome);
        log.info("ProcessStepOutcome {} saved successfully.", stepOutcome.getProcessStepOutcomeId());

        // 10. Build & return response
        EvaluateStepResponse response = buildResponse(stepOutcome, errorMessages);
        log.info("EvaluateStepResponse built successfully: {}", response);
        return response;
    }

    // -------------------------------------------
    // Private Helper Methods
    // -------------------------------------------

    private void validateRequest(EvaluateStepRequest request) {
        if (request.getCorrelationId() == null) {
            throw new IllegalArgumentException("correlationId is required");
        }
        if (request.getTransmissionId() == null) {
            throw new IllegalArgumentException("transmissionId is required");
        }
        if (request.getClientId() == null) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (request.getProcessId() == null) {
            throw new IllegalArgumentException("processId is required");
        }
        if (request.getProcessStepId() == null) {
            throw new IllegalArgumentException("processStepId is required");
        }
    }

    private ProcessEntity findProcess(String processId) {
        return processRepository.findById(processId)
                .orElseThrow(() -> new RuntimeException("Process not found: " + processId));
    }

    private ProcessStep findProcessStep(String processStepId, ProcessEntity processEntity) {
        return processStepRepository.findById(processStepId)
                .orElseThrow(() -> new RuntimeException("ProcessStep not found: " + processStepId));
    }

    private ProcessStepOutcome findOrCreateProcessStepOutcome(EvaluateStepRequest request, ProcessStep processStep) {
        Optional<ProcessStepOutcome> existing = processStepOutcomeRepository
                .findByCorrelationIdAndTransmissionIdAndProcessStepId(
                        request.getCorrelationId(),
                        request.getTransmissionId(),
                        request.getProcessStepId()
                );

        if (existing.isPresent()) {
            log.info("Existing ProcessStepOutcome found for correlationId: {}.", request.getCorrelationId());
            return existing.get();
        }

        // Create new outcome with default nextStepId (non-null, using empty string as default)
        ProcessStepOutcome newOutcome = ProcessStepOutcome.builder()
                .processStepOutcomeId(UUID.randomUUID().toString())
                .correlationId(request.getCorrelationId())
                .transmissionId(request.getTransmissionId())
                .clientId(request.getClientId())
                .processStepId(processStep.getProcessStepId())
                .status(Status.IN_PROGRESS)
                .nextStepId("") // default non-null value
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        log.info("Creating new ProcessStepOutcome with ID: {}", newOutcome.getProcessStepOutcomeId());
        return processStepOutcomeRepository.save(newOutcome);
    }

    private boolean checkClientStepOverride(String clientId, String processStepId) {
        // Uncomment and implement actual override lookup if needed.
        // For now, log and return false.
        log.info("Checking for client step override for clientId: {} and processStepId: {}.", clientId, processStepId);
        return false;
    }

    private void finalizeSkippedStep(ProcessStepOutcome stepOutcome, ProcessStep processStep) {
        log.info("Finalizing skipped step for ProcessStepOutcome {}.", stepOutcome.getProcessStepOutcomeId());
        stepOutcome.setStatus(Status.SKIPPED);
        // Set nextStepId to a default non-null value (empty string)
        stepOutcome.setNextStepId("");
        stepOutcome.setUpdatedAt(new Date());
        processStepOutcomeRepository.save(stepOutcome);
    }

    private void storeActionContext(ProcessStepOutcome stepOutcome, JsonNode payload) {
        String transmissionId = stepOutcome.getTransmissionId();

        // Check if an ActionContext with this transmissionId already exists in the collection.
        Optional<ActionContext> existing = stepOutcome.getActionContexts().stream()
                .filter(ac -> transmissionId.equals(ac.getTransmissionId()))
                .findFirst();

        if (existing.isPresent()) {
            // Update the payload if the ActionContext is already present.
            ActionContext actionContext = existing.get();
            actionContext.setPayload(payload.toString());
            log.info("Updated existing ActionContext with transmissionId: {}", transmissionId);
        } else {
            // Create a new ActionContext and add it.
            ActionContext actionContext = ActionContext.builder()
                    .transmissionId(transmissionId)
                    .processStepOutcomeId(stepOutcome.getProcessStepOutcomeId())
                    .payload(payload.toString())
                    .build();
            stepOutcome.getActionContexts().add(actionContext);
            log.info("Added new ActionContext with transmissionId: {}", transmissionId);
        }
    }

    private List<String> runAttributeRules(ProcessStep processStep, JsonNode payload, ProcessStepOutcome stepOutcome) {
        List<String> errorMessages = new ArrayList<>();

        log.info("Starting rule evaluation for ProcessStep {}.", processStep.getProcessStepId());
        // 1. Find attribute associations for this step
        List<ProcStepAttrAssoc> assocList = procStepAttrAssocRepository
                .findByProcessStepIdAndEnabledIn(processStep.getProcessStepId(), true);
        log.info("Found {} attribute associations.", assocList.size());

        // 2. For each attribute association, get the rule associations
        for (ProcStepAttrAssoc assoc : assocList) {
            List<ProcStepAttrRuleAssoc> ruleAssocs = procStepAttrRuleAssocRepository
                    .findByProcStepAttrAssocIdAndEnabledIn(assoc.getProcStepAttrAssocId(), true);
            log.info("For association {} found {} rule associations.", assoc.getProcStepAttrAssocId(), ruleAssocs.size());

            // 3. Evaluate each rule
            for (ProcStepAttrRuleAssoc ruleAssoc : ruleAssocs) {
                Rule rule = ruleAssoc.getRule();
                boolean passed = ruleExecutionService.evaluate(rule, payload);
                log.info("Evaluating rule '{}' (condition: {}) resulted in: {}", rule.getName(), rule.getRuleCondId(), passed ? "PASSED" : "FAILED");

                if (!passed) {
                    // 4. Get the default message from the rule
                    PrimaryMessage primaryMessage = rule.getDefaultMessage();
                    String msgBody = (primaryMessage != null) ? primaryMessage.getBody() : "Rule failed";

                    // 5. Persist a runtime message using the existing Message entity
                    Message runtimeMsg = Message.builder()
                            .messageId(UUID.randomUUID().toString())
                            .processStepOutcomeId(stepOutcome.getProcessStepOutcomeId())
                            .message(msgBody)
                            .build();
                    messageRepository.save(runtimeMsg);
                    log.info("Saved runtime message for rule failure: {}", msgBody);

                    // 6. Add the error message to the list
                    errorMessages.add(msgBody);
                }
            }
        }

        return errorMessages;
    }

    private String determineNextStepId(ProcessStepOutcome stepOutcome, ProcessStep processStep, List<String> errorMessages) {
        // Example logic: if there are errors, return an empty string; otherwise, return a default next step.
        if (!errorMessages.isEmpty()) {
            log.info("Errors detected. No next step will be set.");
            return "some_fix_error_next_step_id";
        }
        // Replace "some_next_step_id" with logic to determine the actual next step.
        String nextStep = "some_next_step_id";
        log.info("No errors detected. Next step determined as: {}", nextStep);
        return nextStep;
    }

    private EvaluateStepResponse buildResponse(ProcessStepOutcome stepOutcome, List<String> messages) {
        EvaluateStepResponse response = EvaluateStepResponse.builder()
                .correlationId(stepOutcome.getCorrelationId())
                .transmissionId(stepOutcome.getTransmissionId())
                .status(stepOutcome.getStatus())
                .messages(messages)
                .nextStep(stepOutcome.getNextStepId())
                .build();
        log.info("Built EvaluateStepResponse: {}", response);
        return response;
    }
}
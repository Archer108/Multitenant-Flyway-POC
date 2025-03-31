package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.primary.PrimaryMessage;
import com.adp.benefits.carrier.entity.primary.Rule;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class RuleExecutionServiceImpl implements IRuleExecutionService {

    @Override
    public List<String> evaluate(Rule rule, JsonNode payload) {
        String conditionId = rule.getRuleCondId();
        log.info("Evaluating rule '{}' with condition '{}'", rule.getName(), conditionId);
        switch (conditionId) {
            case "cond1":
                log.info("Evaluating SSN validation for condition 'cond1'");
                List<String> ssnErrors = validateSsn(payload);
                List<String> errorMessages = new ArrayList<>();
                if (!ssnErrors.isEmpty()) {
                    PrimaryMessage templateMsg = rule.getDefaultMessage();
                    String template = (templateMsg != null) ? templateMsg.getBody() : "Field {fieldName} is invalid. {errorDetail}";
                    for (String errorDetail : ssnErrors) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("fieldName", "SSN");
                        placeholders.put("errorDetail", errorDetail);
                        String finalMessage = applyPlaceholders(template, placeholders);
                        errorMessages.add(finalMessage);
                    }
                }
                return errorMessages;

            case "cond3":
                log.info("Evaluating Date of Birth validation for condition 'cond3'");
                List<String> dobErrors = validateDob(payload);
                List<String> dobMessages = new ArrayList<>();
                if (!dobErrors.isEmpty()) {
                    PrimaryMessage templateMsg = rule.getDefaultMessage();
                    String template = (templateMsg != null)
                            ? templateMsg.getBody()
                            : "Field {fieldName} is invalid. Error: {errorDetail}";
                    for (String errorDetail : dobErrors) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("fieldName", "Date of Birth");
                        placeholders.put("errorDetail", errorDetail);
                        dobMessages.add(applyPlaceholders(template, placeholders));
                    }
                }
                return dobMessages;

            case "cond2":
                JsonNode coveragePrice = payload.at("/coverage/price");
                List<String> errors = new ArrayList<>();
                if (!coveragePrice.isMissingNode()) {
                    double priceVal = coveragePrice.asDouble(0.0);
                    if (priceVal <= 0.0) {
                        PrimaryMessage templateMsg = rule.getDefaultMessage();
                        String template = (templateMsg != null) ? templateMsg.getBody() : "Coverage price must be greater than 0. Provided: {price}";
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("price", String.valueOf(priceVal));
                        errors.add(applyPlaceholders(template, placeholders));
                    }
                } else {
                    errors.add("Coverage price is missing.");
                }
                return errors;

            default:
                log.warn("Unrecognized rule condition: {}", conditionId);
                return Collections.emptyList();
        }
    }


    private List<String> validateSsn(JsonNode payload) {
        List<String> errors = new ArrayList<>();

        // Validate participant SSN
        JsonNode participantRevisions = payload.at("/participantRevisions");
        if (participantRevisions.isMissingNode() || !participantRevisions.isArray() || participantRevisions.isEmpty()) {
            errors.add("No participant revision found.");
        } else {
            String participantSsn = participantRevisions.get(0).at("/personal/socialSecurityNumber").asText(null);
            if (participantSsn == null || participantSsn.trim().isEmpty()) {
                errors.add("Participant SSN is missing or empty.");
            } else if (isInvalidSsnFormat(participantSsn)) {
                errors.add("Participant SSN '" + participantSsn + "' is not in a valid format.");
            }
        }

        // Validate dependent SSNs
        JsonNode dependents = payload.at("/dependents");
        if (dependents != null && dependents.isArray()) {
            Set<String> dependentSsns = new HashSet<>();
            for (JsonNode dependent : dependents) {
                String depSsn = dependent.at("/personal/socialSecurityNumber").asText(null);
                if (depSsn == null || depSsn.trim().isEmpty()) {
                    errors.add("A dependent SSN is missing or empty.");
                } else if (isInvalidSsnFormat(depSsn)) {
                    errors.add("Dependent SSN '" + depSsn + "' is not in a valid format.");
                }
                if (!dependentSsns.add(depSsn)) {
                    errors.add("Duplicate SSN found among dependents: '" + depSsn + "'.");
                }
            }
            // Check if any dependent SSN equals the participant SSN
            if (!participantRevisions.isMissingNode() && !participantRevisions.isEmpty()) {
                String participantSsn = participantRevisions.get(0).at("/personal/socialSecurityNumber").asText(null);
                if (participantSsn != null && dependentSsns.contains(participantSsn)) {
                    errors.add("Participant SSN '" + participantSsn + "' should not be the same as any dependent SSN.");
                }
            }
        }
        return errors;
    }


    /**
     * Validates the Date of Birth for both participant and dependents.
     *
     * @param payload the JSON payload.
     * @return a list of DOB error messages.
     */
    private List<String> validateDob(JsonNode payload) {
        List<String> errors = new ArrayList<>();
        // Validate participant DOB
        JsonNode participantRevisions = payload.at("/participantRevisions");
        if (participantRevisions.isMissingNode() || !participantRevisions.isArray() || participantRevisions.size() == 0) {
            errors.add("No participant revision found.");
        } else {
            String participantDob = participantRevisions.get(0).at("/personal/dateOfBirth").asText(null);
            if (participantDob == null || participantDob.trim().isEmpty()) {
                errors.add("Participant Date of Birth is missing or empty.");
            } else {
                // Expected format: YYYY-MM-DD
                String dobRegex = "^(19|20)\\d\\d-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";
                if (!participantDob.matches(dobRegex)) {
                    errors.add("Participant Date of Birth '" + participantDob + "' is not in the format YYYY-MM-DD.");
                }
            }
        }

        // Validate dependent DOBs
        JsonNode dependents = payload.at("/dependents");
        if (dependents != null && dependents.isArray()) {
            int depIndex = 0;
            for (JsonNode dependent : dependents) {
                String depDob = dependent.at("/personal/dateOfBirth").asText(null);
                if (depDob == null || depDob.trim().isEmpty()) {
                    errors.add("Dependent " + (depIndex + 1) + " Date of Birth is missing or empty.");
                } else {
                    String dobRegex = "^(19|20)\\d\\d-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";
                    if (!depDob.matches(dobRegex)) {
                        errors.add("Dependent " + (depIndex + 1) + " Date of Birth '" + depDob + "' is not in the format YYYY-MM-DD.");
                    }
                }
                depIndex++;
            }
        }
        return errors;
    }

    private boolean isInvalidSsnFormat(String ssn) {
        String ssnRegex = "^(?!000|666|9\\d{2})\\d{3}-?(?!00)\\d{2}-?(?!0000)\\d{4}$";
        return !ssn.matches(ssnRegex);
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
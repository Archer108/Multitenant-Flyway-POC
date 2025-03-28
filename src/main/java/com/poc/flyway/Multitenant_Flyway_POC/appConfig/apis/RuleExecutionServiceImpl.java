package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.primary.Rule;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RuleExecutionServiceImpl implements IRuleExecutionService {

    @Override
    public boolean evaluate(Rule rule, JsonNode payload) {
        // Example: We check ruleCondId to decide logic
        // This is a placeholder. You'd replace with real logic.

        String conditionId = rule.getRuleCondId();
        log.info("Evaluating rule '{}' with condition '{}'", rule.getName(), conditionId);

        switch (conditionId) {
            case "cond1":
                // e.g., check if SSN field is present and not blank
                JsonNode participantRevisions = payload.at("/participantRevisions");
                if (participantRevisions.isMissingNode() || !participantRevisions.isArray() || participantRevisions.isEmpty()) {
                    log.warn("No participant revisions found in payload.");
                    return false;
                }
                JsonNode ssnNode = participantRevisions.get(0).at("/personal/socialSecurityNumber");
                if (ssnNode.isMissingNode() || ssnNode.asText().isEmpty()) {
                    log.warn("Social Security Number is missing or empty in participant revision.");
                    return false;
                }
                String ssn = ssnNode.asText();
                return validateSsn(ssn, payload);

            case "cond2":
                // e.g., coverage price > 0
                JsonNode coveragePrice = payload.at("/coverage/price");
                if (!coveragePrice.isMissingNode()) {
                    double priceVal = coveragePrice.asDouble(0.0);
                    return (priceVal > 0.0);
                }
                return false;

            default:
                // If we don't recognize the condition, assume it fails or passes?
                log.warn("Unrecognized rule condition: {}", conditionId);
                return true;
        }
    }

    private boolean validateSsn(String ssn, JsonNode payload) {
        // Rule 1: Cannot be null or blank
        if (ssn == null || ssn.trim().isEmpty()) {
            log.warn("SSN is null or empty.");
            return false;
        }

        // Rule 2: Must match SSN format (e.g., 9 digits optionally with dashes)
        // This regex matches SSNs in the format XXX-XX-XXXX or XXXXXXXXX.
        String ssnRegex = "^(?!000|666|9\\d{2})\\d{3}-?(?!00)\\d{2}-?(?!0000)\\d{4}$";
        if (!ssn.matches(ssnRegex)) {
            log.warn("SSN '{}' does not match the required format.", ssn);
            return false;
        }

        // Rule 3: Participant SSN cannot be the same as any dependent's SSN.
        JsonNode dependents = payload.at("/dependents");
        if (dependents != null && dependents.isArray()) {
            for (JsonNode dependent : dependents) {
                JsonNode depSsnNode = dependent.at("/personal/socialSecurityNumber");
                if (!depSsnNode.isMissingNode() && !depSsnNode.asText().isEmpty()) {
                    String depSsn = depSsnNode.asText();
                    if (ssn.equals(depSsn)) {
                        log.warn("Participant SSN '{}' is the same as a dependent's SSN.", ssn);
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
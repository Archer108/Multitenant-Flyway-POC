package com.adp.benefits.carrier.api.service.interfaces;

import com.adp.benefits.carrier.entity.primary.Rule;
import com.adp.benefits.carrier.model.RuleExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;

public interface IRuleExecutionService {

    /**
     * Runs the given rule against the payload+attributeId, measures execution time, and returns a
     * Map containing: • "ruleId" → String • "executionTime" → Long (ms) • "errors" →
     * List<RuleError>
     */
    RuleExecutionResult evaluateWithExecutionTime(Rule rule, String payloadJson, String attributeId)
            throws JsonProcessingException;
}

package com.adp.benefits.carrier.model;

import com.adp.benefits.carrier.model.cpm.models.RuleError;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Wraps a ruleId + executionTime (ms) + zero‐or‐more RuleError. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleExecutionResult {
    private String ruleId;
    private long executionTimeMs;
    private List<RuleError> errors;
    private boolean errored;
    private boolean warning;
}

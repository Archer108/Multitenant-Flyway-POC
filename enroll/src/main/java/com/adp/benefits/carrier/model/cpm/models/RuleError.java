package com.adp.benefits.carrier.model.cpm.models;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleError {
    private String ruleId;
    private String message;
    private List<Map<String, Object>> errorDetails;
    private boolean isWarning;
}

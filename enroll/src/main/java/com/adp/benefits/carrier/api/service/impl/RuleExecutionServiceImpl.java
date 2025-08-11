// src/main/java/com/adp/benefits/carrier/api/service/impl/RuleExecutionServiceImpl.java
package com.adp.benefits.carrier.api.service.impl;

import com.adp.benefits.carrier.api.service.interfaces.IRuleExecutionService;
import com.adp.benefits.carrier.api.service.rule.AttributeMappingLoader;
import com.adp.benefits.carrier.api.service.rule.RuleTemplateEvaluator;
import com.adp.benefits.carrier.entity.primary.AttributeList;
import com.adp.benefits.carrier.entity.primary.Rule;
import com.adp.benefits.carrier.entity.primary.repository.AttributeListRepository;
import com.adp.benefits.carrier.entity.primary.repository.PrimaryMessageRepository;
import com.adp.benefits.carrier.model.RuleExecutionResult;
import com.adp.benefits.carrier.model.cpm.models.RuleError;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RuleExecutionServiceImpl implements IRuleExecutionService {

    private final PrimaryMessageRepository messageRepo;
    private final AttributeListRepository attributeRepo;
    private final ObjectMapper mapper;
    private final AttributeMappingLoader mappingLoader;
    private final RuleTemplateEvaluator evaluator;

    @Override
    public RuleExecutionResult evaluateWithExecutionTime(
            Rule rule, String payloadJson, String attributeId) throws JsonProcessingException {
        long start = System.currentTimeMillis();

        // 1) parse once
        JsonNode root = mapper.readTree(payloadJson);
        @SuppressWarnings("unchecked")
        Map<String, Object> context = mapper.convertValue(root, Map.class);
        var spelCtx = new StandardEvaluationContext(context);
        spelCtx.setVariable("payload", context);

        // 2) normalize
        String tpl =
                Optional.ofNullable(rule.getRuleExpression())
                        .map(s -> s.replaceAll("\\s+", " ").trim())
                        .orElse("");

        List<RuleError> errors = new ArrayList<>();

        // 3) for each mapping entry that *actually exists* in payload
        var entries = mappingLoader.getEntries(attributeId);
        if (tpl.contains("{jsonPointer}") && !entries.isEmpty()) {
            for (var me : entries) {
                Object res = evaluator.evaluate(tpl, me, context);

                // convert both boolean(false) and List<Map> into List<RuleError>
                collectErrors(rule, me.getLabel(), res, errors);
            }
        } else {
            // no pointer‐templating → single evaluation
            Object res = evaluator.evaluate(tpl, null, context);
            String label =
                    attributeRepo.findByAttributeId(attributeId).stream()
                            .map(AttributeList::getAttributeName)
                            .findFirst()
                            .orElse(attributeId);
            collectErrors(rule, label, res, errors);
        }

        long took = System.currentTimeMillis() - start;
        return RuleExecutionResult.builder()
                .ruleId(rule.getRuleId())
                .executionTimeMs(took)
                .errors(errors)
                .build();
    }

    @SuppressWarnings("unchecked")
    private void collectErrors(Rule rule, String label, Object result, List<RuleError> out) {
        // 1) if boolean false → no detail maps
        if (result instanceof Boolean b) {
            if (!b) {
                out.add(
                        RuleError.builder()
                                .ruleId(rule.getRuleId())
                                .message(loadMessage(rule, label))
                                .errorDetails(Collections.emptyList())
                                .build());
            }
            return;
        }

        // 2) if list of maps → dynamic details
        if (result instanceof List<?> raw && !raw.isEmpty()) {
            var vio = raw.stream().map(o -> (Map<String, Object>) o).collect(Collectors.toList());
            out.add(
                    RuleError.builder()
                            .ruleId(rule.getRuleId())
                            .message(loadMessage(rule, label))
                            .errorDetails(vio)
                            .build());
        }
    }

    private String loadMessage(Rule rule, String label) {
        return messageRepo
                .findById(rule.getDefaultMessage().getMessageId())
                .map(pm -> pm.getBody().replace("{attributeName}", label))
                .orElse(rule.getName());
    }
}

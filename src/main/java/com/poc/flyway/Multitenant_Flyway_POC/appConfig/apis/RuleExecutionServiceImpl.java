package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.AttributeMappingLoader;
import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.primary.AttributeList;
import com.adp.benefits.carrier.entity.primary.Rule;
import com.adp.benefits.carrier.entity.primary.repository.AttributeListRepository;
import com.adp.benefits.carrier.entity.primary.repository.PrimaryMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleExecutionServiceImpl implements IRuleExecutionService {

    private final PrimaryMessageRepository messagesRepository;
    private final AttributeListRepository attributeListRepository;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser;

    public RuleExecutionServiceImpl(
            PrimaryMessageRepository messagesRepository,
            AttributeListRepository attributeListRepository) {
        this.messagesRepository = messagesRepository;
        this.attributeListRepository = attributeListRepository;
        this.objectMapper = new ObjectMapper();
        this.parser = new SpelExpressionParser();
    }

    @Override
    public List<String> evaluate(Rule rule, String payload) {
        try {
            Map<String, Object> rootMap = parsePayload(payload);
            System.out.println("🧠 Parsed Payload (rootMap): " + rootMap.keySet());
            StandardEvaluationContext context = createContext(rootMap);

            String ruleExpression = normalize(rule.getRuleExpression());
            List<String> allErrors = new ArrayList<>();

            log.info("🚦 Executing Rule: ID = {}, Name = {}, Expression = {}",
                    rule.getRuleId(),
                    rule.getName(),
                    normalize(rule.getRuleExpression()));

            String attributeId =
                    rule.getProcStepAttrRuleAssocs().stream()
                            .map(assoc -> assoc.getProcStepAttrAssoc().getAttribute().getAttributeId())
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("No attributeId found"));

            if (ruleExpression.contains("{attributePath")) {
                List<String> resolvedPaths = AttributeMappingLoader.mapAttributesPaths(attributeId, rootMap);
                log.info("📍 Checking valid paths in payload for attributeId {}: Found => {}", attributeId, resolvedPaths);
                log.info("Resolved paths for attributeId {}: {}", attributeId, resolvedPaths);
                if (resolvedPaths.isEmpty()) {
                    throw new IllegalArgumentException("No valid attribute path in payload for attributeId: " + attributeId);
                }
                for (String resolvedPath : resolvedPaths) {
                    String mappingKey = attributeId + "." + resolvedPath;
                    String attributeDesc = AttributeMappingLoader.getMapping(attributeId, resolvedPath);
                    if (attributeDesc == null) {
                        log.warn("No mapping found for key {}. Falling back to default resolution.", mappingKey);
                        attributeDesc = resolveAttributeName(attributeId, resolvedPath);
                    }
                    String resolvedExpression = ruleExpression.replace("{attributePath}", resolvedPath);
                    log.info("🔧 Evaluating expression for rule {}: {}", rule.getRuleId(), resolvedExpression);
                    Object result = safeEvaluate(resolvedExpression, context);
                    allErrors.addAll(processResult(rule, result, attributeId, resolvedPath, attributeDesc));
                }
            } else {
                Object result = safeEvaluate(ruleExpression, context);
                // 🧠 Attempt to detect the attributePath from result if possible
                String detectedPath = detectRootNodeFromResult(result, rootMap);
                allErrors.addAll(processResult(rule, result, attributeId, detectedPath, null));
            }
            return allErrors;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Object> evaluateWithExecutionTime(Rule rule, String payloadString) {
        long startTime = System.currentTimeMillis();
        List<String> evaluationResult = evaluate(rule, payloadString);
        long endTime = System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("ruleId", rule.getRuleId());
        result.put("executionTime", endTime - startTime);
        result.put("errors", evaluationResult);

        return result;
    }

    private List<String> processResult(
            Rule rule,
            Object result,
            String attributeId,
            String attributePath,
            String attributeDesc) {
        if (result instanceof Boolean boolResult) {
            return handleBooleanResult(rule, boolResult, attributeId, attributePath, attributeDesc);
        }

        if (result instanceof List<?> listResult && !listResult.isEmpty()) {
            return handleListResult(rule, listResult, attributeId, attributePath, attributeDesc);
        }

        return Collections.emptyList();
    }

    private List<String> handleBooleanResult(
            Rule rule,
            Boolean passed,
            String attributeId,
            String attributePath,
            String attributeDesc) {
        if (!passed) {
            String desc =
                    attributeDesc != null
                            ? attributeDesc
                            : resolveAttributeName(attributeId, attributePath);
            return List.of(formatMessage(rule, "\"Rule evaluated to false\"", desc));
        }
        return Collections.emptyList();
    }

    private List<String> handleListResult(
            Rule rule,
            List<?> violations,
            String attributeId,
            String attributePath,
            String attributeDesc) {
        String readable = convertToHumanReadable(violations);
        String desc =
                attributeDesc != null
                        ? attributeDesc
                        : resolveAttributeName(attributeId, attributePath);
        return List.of(formatMessage(rule, readable, desc));
    }

    private Map<String, Object> parsePayload(String payload) throws Exception {
        return objectMapper.readValue(payload, new TypeReference<>() {});
    }

    private StandardEvaluationContext createContext(Map<String, Object> rootMap) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);
        return context;
    }

    private String resolveAttributeName(String attributeId, String attributePath) {
        List<AttributeList> attributes = attributeListRepository.findByAttributeId(attributeId);
        if (attributes.isEmpty()) {
            return "Unknown Attribute";
        }

        // 🧠 Case 1: If the rule expression uses {attributePath}, then attributePath is NOT null
        if (attributePath != null && !attributePath.isEmpty()) {
            String roleLabel = AttributeMappingLoader.getMapping(attributeId, attributePath); // Participant / Dependent etc.
            if (roleLabel != null) {
                return attributes.stream()
                        .map(AttributeList::getAttributeName)
                        .filter(name -> name.toLowerCase().contains(roleLabel.toLowerCase()))
                        .findFirst()
                        .orElse(attributes.get(0).getAttributeName());
            }
        }

        // 🧠 Case 2: No placeholder — just return first match
        return attributes.get(0).getAttributeName();
    }

    private String formatMessage(Rule rule, String errorDetails, String attributeDescriptor) {
        return Optional.ofNullable(rule.getDefaultMessage())
                .map(msg -> resolveMessage(msg.getMessageId(), errorDetails, attributeDescriptor))
                .orElse("Validation failed: missing default message for rule " + rule.getRuleId());
    }

    private String resolveMessage(
            String messageId, String errorDetails, String attributeDescriptor) {

        return messagesRepository
                .findById(messageId)
                .map(msg -> {
                    String messageBody = msg.getBody();
                    if (messageBody.contains("{errorDetails}")) {
                        messageBody = messageBody.replace("{errorDetails}", errorDetails);
                    }
                    if (messageBody.contains("{attributeName}")) {
                        messageBody = messageBody.replace("{attributeName}", attributeDescriptor);
                    }
                    return messageBody;
                })
                .orElse("Validation failed: missing message for rule " + messageId);
    }


    private String normalize(String expr) {
        return Optional.ofNullable(expr)
                .map(
                        e ->
                                e.replace("\\n", "\n")
                                        .replace("\\r", "\r")
                                        .replace("\\t", "\t")
                                        .replace("\\\"", "\"")
                                        .replace("\\\\", "\\")
                                        .replaceAll("\\s+", " ")
                                        .trim())
                .orElse("");
    }

    private String convertToHumanReadable(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            return "{"
                    + map.entrySet().stream()
                    .map(e -> e.getKey() + ": " + convertToHumanReadable(e.getValue()))
                    .collect(Collectors.joining(", "))
                    + "}";
        } else if (obj instanceof List<?> list) {
            return "["
                    + list.stream()
                    .map(this::convertToHumanReadable)
                    .collect(Collectors.joining(", "))
                    + "]";
        } else {
            return String.valueOf(obj);
        }
    }

    private Object safeEvaluate(String expression, StandardEvaluationContext context) {
        try {
            return parser.parseExpression(expression).getValue(context);
        } catch (SpelEvaluationException e) {
            log.error("Error Evaluating Expression [{}]: {}", expression, e.getMessage());
            throw e;
        }
    }

    private String detectRootNodeFromResult(Object result, Map<String, Object> rootMap) {
        if (!(result instanceof List<?> list) || list.isEmpty()) return "";

        String firstItemStr = convertToHumanReadable(list.get(0)).toLowerCase();

        for (Map.Entry<String, Object> entry : rootMap.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof List<?> entryList) {
                for (Object item : entryList) {
                    if (convertToHumanReadable(item).toLowerCase().contains(firstItemStr)) {
                        return entry.getKey(); // e.g. "dependents"
                    }
                }
            }
        }
        return "";
    }
}

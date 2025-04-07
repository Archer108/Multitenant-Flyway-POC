package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.api.service.IRuleExecutionService;
import com.adp.benefits.carrier.entity.primary.Rule;
import com.adp.benefits.carrier.entity.primary.repository.PrimaryMessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleExecutionServiceImpl implements IRuleExecutionService {

    private final PrimaryMessageRepository messagesRepository;
    private final ObjectMapper objectMapper;
    private final ExpressionParser parser;

    public RuleExecutionServiceImpl(PrimaryMessageRepository messagesRepository) {
        this.messagesRepository = messagesRepository;
        this.objectMapper = new ObjectMapper();
        this.parser = new SpelExpressionParser();
    }

    @Override
    public List<String> evaluate(Rule rule, String payload) {
        try {
            Map<String, Object> rootMap = parsePayload(payload);
            StandardEvaluationContext context = createContext(rootMap);

            String normalizedExpression = normalize(rule.getRuleExpression());
            Object result = parser.parseExpression(normalizedExpression).getValue(context);

            return processResult(rule, result);

        } catch (Exception e) {
            log.error("Rule evaluation failed for ruleId [{}]: {}", rule.getRuleId(), e.getMessage(), e);
            return List.of("Rule evaluation failed: " + e.getMessage());
        }
    }

    private Map<String, Object> parsePayload(String payload) throws Exception {
        return objectMapper.readValue(payload, new TypeReference<>() {});
    }

    private StandardEvaluationContext createContext(Map<String, Object> rootMap) {
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);
        return context;
    }

    private List<String> processResult(Rule rule, Object result) {
        if (result instanceof Boolean boolResult) {
            return handleBooleanResult(rule, boolResult);
        }

        if (result instanceof List<?> listResult && !listResult.isEmpty()) {
            return handleListResult(rule, listResult);
        }

        return Collections.emptyList();
    }

    private List<String> handleBooleanResult(Rule rule, boolean passed) {
        if (!passed) {
            return List.of(formatMessage(rule, "\"Rule evaluated to false\""));
        }
        return Collections.emptyList();
    }

    private List<String> handleListResult(Rule rule, List<?> violations) {
        String readable = convertToHumanReadable(violations);
        return List.of(formatMessage(rule, readable));
    }

    private String formatMessage(Rule rule, String errorDetails) {
        return Optional.ofNullable(rule.getDefaultMessage())
                .map(msg -> resolveMessage(msg.getMessageId(), errorDetails))
                .orElse("Validation failed: missing default message for rule " + rule.getRuleId());
    }

    private String resolveMessage(String messageId, String replacement) {
        return messagesRepository.findById(messageId)
                .map(msg -> msg.getBody().replace("{errorDetails}", replacement))
                .orElse("Validation failed: missing message for rule " + messageId);
    }

    private String normalize(String expr) {
        return Optional.ofNullable(expr)
                .map(e -> e.replace("\\n", "\n")
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
            return map.entrySet().stream()
                    .map(e -> e.getKey() + ": " + convertToHumanReadable(e.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        } else if (obj instanceof List<?> list) {
            return list.stream()
                    .map(this::convertToHumanReadable)
                    .collect(Collectors.joining(", ", "[", "]"));
        } else {
            return String.valueOf(obj);
        }
    }
}
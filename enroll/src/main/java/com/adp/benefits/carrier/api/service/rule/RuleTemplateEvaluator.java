package com.adp.benefits.carrier.api.service.rule;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.Map;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

/**
 * Renders a SpEL template for a given MappingEntry, caches the parsed Expression, and evaluates it
 * against the provided context.
 */
@Component
public class RuleTemplateEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final LoadingCache<RuleMappingKey, Expression> exprCache =
            Caffeine.newBuilder()
                    .maximumSize(5_000)
                    .expireAfterWrite(Duration.ofMinutes(30))
                    .build(
                            key -> {
                                String payloadPath =
                                        key.getJsonPointer().replaceAll("/(\\w+)", "['$1']");
                                String leaf =
                                        key.getLeafPointer() == null
                                                        || key.getLeafPointer().isBlank()
                                                ? "#this"
                                                : "#this"
                                                        + key.getLeafPointer()
                                                                .replaceAll("/(\\w+)", "['$1']");
                                String spelTpl =
                                        key.getTemplate()
                                                .replace(
                                                        "#payload{jsonPointer}",
                                                        "#payload" + payloadPath)
                                                .replace("#this{leafPointer}", leaf);
                                return parser.parseExpression(spelTpl);
                            });

    /**
     * Evaluate the given raw template (with placeholders) for the MappingEntry against the context
     * map.
     */
    public Object evaluate(
            String rawTemplate,
            AttributeMappingLoader.MappingEntry me,
            Map<String, Object> context) {
        // 1) inline label and regex
        String filled =
                rawTemplate
                        .replace("{label}", me.getLabel())
                        .replace(
                                "{regex}",
                                me.getRegex() == null || me.getRegex().isBlank()
                                        ? ".*"
                                        : me.getRegex());
        // 2) cache/parse
        RuleMappingKey key = new RuleMappingKey(filled, me);
        Expression expr = exprCache.get(key);
        // 3) evaluate
        StandardEvaluationContext ctx = new StandardEvaluationContext(context);
        ctx.setVariable("payload", context);
        return expr.getValue(ctx);
    }
}

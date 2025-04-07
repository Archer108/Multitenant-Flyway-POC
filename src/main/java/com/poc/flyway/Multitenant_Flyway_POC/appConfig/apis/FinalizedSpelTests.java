package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FinalizedSpelTests {

    private Map<String, Object> rootMap;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() throws Exception {
        // Load payload from src/test/resources/RuleValidationTestPayload.json
        try (InputStream is = getClass().getResourceAsStream("/RuleValidationTestPayload.json")) {
            if (is == null) {
                throw new RuntimeException("File RuleValidationTestPayload.json not found in src/test/resources");
            }
            rootMap = objectMapper.readValue(is, new TypeReference<Map<String, Object>>() {});
        }
    }

    public String buildMessage(String template, List<Map<String, Object>> violations) {
        try {
            String json = objectMapper.writeValueAsString(violations);
            return template.replace("{errorDetails}", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize error details", e);
        }
    }

    @Test
    public void testParticipantSSNWithNullSsn() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String ruleSpel =
                """
                            #payload['participantRevisions']
                                .![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                                    'ssn': #this['personal']['socialSecurityNumber']}]
                                .?[#this['ssn'] == null or #this['ssn'].isEmpty()]
                        """;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> violations =
                (List<Map<String, Object>>) parser.parseExpression(ruleSpel).getValue(context);

        if (violations != null && !violations.isEmpty()) {
            String templateFromDb = "Participant SSN is NULL: {errorDetails}";
            String finalMessage = buildMessage(templateFromDb, violations);

            fail(finalMessage); // or return response with that message
        }
    }

    @Test
    public void testParticipantSSNFormat_SpelOnly() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String spel =
                """
                        #payload['participantRevisions']
                            .![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                                'ssn': #this['personal']['socialSecurityNumber']}]
                            .?[#this['ssn'] != null and !#this['ssn'].isEmpty() and !#this['ssn'].matches('^(\\d{3}-\\d{2}-\\d{4}|\\d{9})$')]
                        """;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidSSNs =
                (List<Map<String, Object>>) parser.parseExpression(spel).getValue(context);

        if (invalidSSNs != null && !invalidSSNs.isEmpty()) {
            String template = "Participant SSN is in invalid format: {errorDetails}";
            String finalMessage = buildMessage(template, invalidSSNs);
            fail(finalMessage);
        }
    }


    @Test
    public void testDuplicateSSNs_ParticipantAndDependent_WithNormalization() throws Exception {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String spel =
                "T(org.apache.commons.collections4.CollectionUtils).union(" +
                        "#payload['participantRevisions']" +
                        ".![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}]" +
                        ".?[T(org.apache.commons.collections4.CollectionUtils).intersection(" +
                        "#payload['participantRevisions'].![#this['personal']['socialSecurityNumber'] != null ? #this['personal']['socialSecurityNumber'].replace('-', '') : ''], " +
                        "#payload['dependents'].![#this['personal']['socialSecurityNumber'] != null ? #this['personal']['socialSecurityNumber'].replace('-', '') : '']" +
                        ").contains(#this['ssn'] != null ? #this['ssn'].replace('-', '') : '')]," +
                        "#payload['dependents']" +
                        ".![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}]" +
                        ".?[T(org.apache.commons.collections4.CollectionUtils).intersection(" +
                        "#payload['participantRevisions'].![#this['personal']['socialSecurityNumber'] != null ? #this['personal']['socialSecurityNumber'].replace('-', '') : ''], " +
                        "#payload['dependents'].![#this['personal']['socialSecurityNumber'] != null ? #this['personal']['socialSecurityNumber'].replace('-', '') : '']" +
                        ").contains(#this['ssn'] != null ? #this['ssn'].replace('-', '') : '')])";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> duplicates =
                (List<Map<String, Object>>) parser.parseExpression(spel).getValue(context);

        if (duplicates != null && !duplicates.isEmpty()) {
            String template = "One of participant or dependent SSN is duplicate: {errorDetails}";
            String finalMessage = buildMessage(template, duplicates);
            fail(finalMessage);
        }
    }


    @Test
    public void testAllDependentSSNNotNullOrEmpty() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                "#payload['dependents'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], " +
                        "'ssn': #this['personal'] != null ? #this['personal']['socialSecurityNumber'] : null}]" +
                        ".?[#this['ssn'] == null or #this['ssn'] == '']";

        List<?> invalidDependents =
                parser.parseExpression(expression).getValue(context, List.class);

        assertTrue(
                invalidDependents.isEmpty(),
                "Dependent SSN is NULL or empty: " + invalidDependents);
    }

    @Test
    public void testDependentSSNFormat_SpelOnly() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String spel =
                """
                #payload['dependents']
                    .![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                        'ssn': #this['personal']['socialSecurityNumber']}]
                    .?[#this['ssn'] != null 
                       and !#this['ssn'].isEmpty() 
                       and !#this['ssn'].matches('^([0-9]{3}-[0-9]{2}-[0-9]{4}|[0-9]{9})$')]
                """;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidSSNs =
                (List<Map<String, Object>>) parser.parseExpression(spel).getValue(context);

        if (invalidSSNs != null && !invalidSSNs.isEmpty()) {
            String template = "Dependent SSN is in invalid format: {errorDetails}";
            String finalMessage = buildMessage(template, invalidSSNs);
            fail(finalMessage);
        }
    }

    @Test
    public void testDuplicateDependentSSNUsingSingleSpEL() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                "#payload['dependents'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], " +
                        "'ssn': #this['personal']['socialSecurityNumber']}]" +
                        ".?[T(java.util.Collections).frequency(" +
                        "#payload['dependents'].![#this['personal']['socialSecurityNumber'] != null ? #this['personal']['socialSecurityNumber'].replace('-', '') : ''], " +
                        "#this['ssn'] != null ? #this['ssn'].replace('-', '') : ''" +
                        ") > 1]";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> duplicates =
                (List<Map<String, Object>>) parser.parseExpression(expression).getValue(context, List.class);

        if (duplicates != null && !duplicates.isEmpty()) {
            String template = "Duplicate dependent SSNs: {errorDetails}";
            String finalMessage = buildMessage(template, duplicates);
            fail(finalMessage);
        }
    }


    @Test
    public void testGenderValuesAndNamesAreMOrF() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                "T(org.apache.commons.collections4.CollectionUtils).union(" +
                        "T(org.apache.commons.collections4.CollectionUtils).union(" +
                        "#payload['participantRevisions'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'gender': #this['personal']['gender']}], " +
                        "#payload['dependents'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'gender': #this['personal']['gender']}]" +
                        "), " +
                        "#payload['beneficiaries'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'gender': #this['person']['gender']}]" +
                        ").?[!(#this['gender'] == 'M' or #this['gender'] == 'F')]";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidEntries = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Invalid gender entries: " + invalidEntries);

        if (invalidEntries != null && !invalidEntries.isEmpty()) {
            String template = "Invalid gender values found. The following person(s) have invalid gender: {errorDetails}. Allowed gender values are only 'M' or 'F'.";
            String finalMessage = buildMessage(template, invalidEntries);
            fail(finalMessage);
        }
    }

    @Test
    public void testGenderValuesAndNamesAreMOrForMaleOrFemale() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                "T(org.apache.commons.collections4.CollectionUtils).union(" +
                        "T(org.apache.commons.collections4.CollectionUtils).union(" +
                        "#payload['participantRevisions'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'gender': #this['personal']['gender']}], " +
                        "#payload['dependents'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'gender': #this['personal']['gender']}]" +
                        "), " +
                        "#payload['beneficiaries'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'gender': #this['person']['gender']}]" +
                        ").?[!(#this['gender'] == 'M' or #this['gender'] == 'F' or #this['gender'] == 'Male' or #this['gender'] == 'Female')]";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> invalidEntries = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Invalid gender entries: " + invalidEntries);

        if (invalidEntries != null && !invalidEntries.isEmpty()) {
            String template = "Invalid gender values found. The following person(s) have invalid gender: {errorDetails}. Allowed gender values are 'M', 'F', 'Male', or 'Female'.";
            String finalMessage = buildMessage(template, invalidEntries);
            fail(finalMessage);
        }
    }

    @Test
    public void testChildDependentAgeNotGreaterThanParticipantAge() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                "#payload['dependents']" +
                        ".?[#this['personal']['relationship'] == 'Child' " +
                        "and T(java.time.LocalDate).parse(#this['personal']['dateOfBirth']) <= T(java.time.LocalDate).parse(" +
                        "#payload['participantRevisions'][0]['personal']['dateOfBirth'])]" +
                        ".![{'firstName': #this['name']['firstName'], 'lastName': #this['name']['lastName'], " +
                        "'dateOfBirth': #this['personal']['dateOfBirth']}]";

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> failingDependents = parser.parseExpression(expression).getValue(context, List.class);

        if (failingDependents != null && !failingDependents.isEmpty()) {
            String template = "Found child dependents with birth date on or before participant birth date: {errorDetails}";
            String finalMessage = buildMessage(template, failingDependents);
            fail(finalMessage);
        }
    }


    @Test
    public void testPlanTypesValidFromEarlierThanValidTo_withRuleExecutionLogic() throws Exception {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                "#payload['planTypes'].![#this['enrollmentRevisions'].![#this]"
                        + ".?[T(java.time.LocalDate).parse(#this['validFrom']).isAfter(T(java.time.LocalDate).parse(#this['validTo'])) "
                        + "or T(java.time.LocalDate).parse(#this['validFrom']).isEqual(T(java.time.LocalDate).parse(#this['validTo']))]"
                        + ".![{'validFrom': #this['validFrom'], 'validTo': #this['validTo']}]]"
                        + ".![#this].?[!#this.empty]";

        Object result = parser.parseExpression(expression).getValue(context);

        if (result instanceof Boolean boolResult) {
            if (!boolResult) {
                String msg = resolveTestMessage("validFrom >= validTo", "\"Rule evaluated to false\"");
                fail(msg);
            }
            // ✅ Test passes silently
        } else if (result instanceof List<?> listResult && !listResult.isEmpty()) {
            String humanReadable = convertToHumanReadable(listResult);
            String msg = resolveTestMessage("validFrom >= validTo", humanReadable);
            fail(msg);
        }
    }


    @Test
    public void testSpecific_PlanTypes_AnnualGreaterThanPayPeriod_IfPreTaxPresent() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                """
                    #payload['planTypes']
                        .?[#this['standardBenefitArea'] == 'MEDICAL']
                        .![{'annual': #this['enrollmentRevisions'][0]['enrollment']['current']['costs']['participantAmount']['preTax']['annual'],
                            'payPeriod': #this['enrollmentRevisions'][0]['enrollment']['current']['costs']['participantAmount']['preTax']['payPeriod']}]
                        .?[#this['annual'] <= #this['payPeriod']]
                """;


        Object result = parser.parseExpression(expression).getValue(context);

        if (result instanceof Boolean boolResult) {
            if (!boolResult) {
                String msg = resolveTestMessage("Annual >= PayPeriod", "\"Rule evaluated to false\"");
                fail(msg);
            }
            // ✅ Test passes silently
        } else if (result instanceof List<?> listResult && !listResult.isEmpty()) {
            String humanReadable = convertToHumanReadable(listResult);
            String msg = resolveTestMessage("Annual >= PayPeriod", humanReadable);
            fail(msg);
        }
    }

    @Test
    public void testAllPlanTypes_AnnualGreaterThanPayPeriod_IfPreTaxPresent() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("payload", rootMap);

        String expression =
                """
                #payload['planTypes']
                    .![#this['enrollmentRevisions'][0]['enrollment']['current']['costs']['participantAmount']['preTax'] != null ?
                        {'annual': #this['enrollmentRevisions'][0]['enrollment']['current']['costs']['participantAmount']['preTax']['annual'],
                         'payPeriod': #this['enrollmentRevisions'][0]['enrollment']['current']['costs']['participantAmount']['preTax']['payPeriod'],
                         'benefitArea': #this['standardBenefitArea']} : null]
                    .?[#this != null and #this['annual'] <= #this['payPeriod']]
                """;

        Object result = parser.parseExpression(expression).getValue(context);

        if (result instanceof Boolean boolResult) {
            if (!boolResult) {
                String msg = resolveTestMessage("Annual >= PayPeriod", "\"Rule evaluated to false\"");
                fail(msg);
            }
            // ✅ Test passes silently
        } else if (result instanceof List<?> listResult && !listResult.isEmpty()) {
            String humanReadable = convertToHumanReadable(listResult);
            String msg = resolveTestMessage("Annual >= PayPeriod", humanReadable);
            fail(msg);
        }
    }


    private String resolveTestMessage(String ruleName, String details) {
        return "❌ Rule [" + ruleName + "] failed. Details: " + details;
    }


    private String convertToHumanReadable(Object obj) {
        if (obj == null) {
            return "null";
        } else if (obj instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + convertToHumanReadable(entry.getValue()))
                    .collect(Collectors.joining(", ", "{", "}"));
        } else if (obj instanceof List<?> list) {
            return list.stream()
                    .map(this::convertToHumanReadable)
                    .collect(Collectors.joining(", ", "[", "]"));
        } else {
            return obj.toString();
        }
    }

}

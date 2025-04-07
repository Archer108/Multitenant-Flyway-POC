package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class SpelTests {

    private static final String SAMPLE_PAYLOAD =
            """
            {
              "correlationId" : "11111111-1111-1111-1111-123456789012",
              "transmissionId": "21111111-1111-1111-2211-123456789012",
              "aoid": "98BE5B782270036B",
              "clientId": "11111111-1111-1111-1111-111111111111",
              "systemId": "HOS",
              "processId": "hwse_event_processing",
              "processStepId": "publish_hwse_payload",
              "payload": {
                "aoid": "G37RYD2H1A7BD8BF",
                "ooid": "98BE5B782270036B",
                "correlationId": "12345678-4444-2222-3333-123456789012",
                "schemaVersion": "1.00",
                "beneficiaries": [
                  {
                    "beneficiaryInfo": {
                      "beneficiaryNumber": 0,
                      "beneficiaryType": "Person",
                      "relationship": "Child1"
                    },
                    "contact": {
                      "addresses": [
                        {
                          "street": "123 Main St",
                          "city": "Anytown",
                          "state": "CA",
                          "postalCode": "12345",
                          "country": "USA"
                        }
                      ]
                    },
                    "hash": "fwerer4545324",
                    "name": {
                      "firstName": "John",
                      "lastName": "Doe"
                    },
                    "person": {
                      "dateOfBirth": "1990-01-01",
                      "gender": "Male",
                      "socialSecurityNumber": "224-56-6898"
                    }
                  },
                  {
                    "beneficiaryInfo": {
                      "beneficiaryNumber": 0,
                      "beneficiaryType": "Person",
                      "relationship": "Child2"
                    },
                    "contact": {
                      "addresses": [
                        {
                          "street": "123 Main St",
                          "city": "Anytown",
                          "state": "CA",
                          "postalCode": "12345",
                          "country": "USA"
                        }
                      ]
                    },
                    "hash": "fwerer4545365",
                    "name": {
                      "firstName": "Simran",
                      "lastName": "Doe"
                    },
                    "person": {
                      "dateOfBirth": "1990-01-01",
                      "gender": "F",
                      "socialSecurityNumber": "224-56-6898"
                    }
                  }
                ],
                "dependents": [
                  {
                    "contacts": {
                      "addresses": [
                        {
                          "street": "456 Elm St",
                          "city": "Anytown",
                          "state": "CA",
                          "postalCode": "12345",
                          "country": "USA"
                        }
                      ]
                    },
                    "name": {
                      "firstName": "Janet",
                      "lastName": "Doe"
                    },
                    "personal": {
                      "dateOfBirth": "2010-01-01",
                      "gender": "FM",
                      "relationship": "Child",
                      "socialSecurityNumber": "447-11-9876",
                      "student": true,
                      "contactNumber": {
                        "phone": [
                          {
                            "type": "Home",
                            "number": "123-456-7890"
                          },
                          {
                            "type": "Work",
                            "number": "987-654-3210"
                          }
                        ]
                      }
                    },
                    "hash": "werer4545324",
                    "medicare": {}
                  },
                  {
                    "contacts": {
                      "addresses": [
                        {
                          "street": "456 Elm St",
                          "city": "Anytown",
                          "state": "CA",
                          "postalCode": "12345",
                          "country": "USA"
                        }
                      ]
                    },
                    "name": {
                      "firstName": "Jennifer",
                      "lastName": "Doe"
                    },
                    "personal": {
                      "dateOfBirth": "1980-01-01",
                      "gender": "F",
                      "relationship": "Spouse",
                      "socialSecurityNumber": "447-11-9876",
                      "student": true
                    },
                    "hash": "werer4545324",
                    "medicare": {}
                  },
                  {
                    "contacts": {
                      "addresses": [
                        {
                          "street": "456 Elm St",
                          "city": "Anytown",
                          "state": "CA",
                          "postalCode": "12345",
                          "country": "USA"
                        }
                      ]
                    },
                    "name": {
                      "firstName": "Johny",
                      "lastName": "Doe"
                    },
                    "personal": {
                      "dateOfBirth": "1980-01-01",
                      "gender": "M",
                      "relationship": "Child",
                      "socialSecurityNumber": "547-11-9876",
                      "student": true,
                      "contactNumber": {
                        "phone": [
                          {
                            "type": "Home",
                            "number": "123-456-7860"
                          },
                          {
                            "type": "Work",
                            "number": "987-654-3310"
                          }
                        ]
                      }
                    },
                    "hash": "werer4545324",
                    "medicare": {}
                  }
                ],
                "participantRevisions": [
                  {
                    "contact": {
                      "addresses": [
                        {
                          "street": "789 Oak St",
                          "city": "Anytown",
                          "state": "CA",
                          "postalCode": "12345",
                          "country": "USA"
                        }
                      ],
                      "emailAddress": "johndoe@example.com",
                      "phoneNumbers":[
                        {
                          "phoneNumberType": "Home",
                          "phoneNumber": "123-456-7890"
                        },
                        {
                          "phoneNumberType": "Work",
                          "phoneNumber": "987-654-3210"
                        }
                      ]
                    },
                    "custom": {
                      "clientDefinitionOrganizationCode": "1234567890",
                      "clientDefinedStatus": "Info"
                    },
                    "employment": {
                      "employmentStatus": "Employed",
                      "employmentType": "Full-Time",
                      "occupation": "Software Developer",
                      "benefitEligibilityGroup": "003",
                      "participantType": "AE"
                    },
                    "hash": "werer4545324",
                    "id": {
                    "employeeId": "000000YUK",
                      "fileNumber": "12334565",
                      "participantId": "GHFY89JKU01",
                      "subscriberType": "AE"
                    },
                    "job": {
                      "jobCode": "01-5050"
                    },
                    "leave": {
                      "leaveType": "L",
                      "scheduleReturnWork": "2024-12-31",
                      "leaveOfAbsenseStartDate": "2024-12-30"
                    },
                    "medicare": {
                      "eligibleReason": "N"
                    },
                    "name": {
                      "firstName": "Johns",
                      "lastName": "Doe",
                      "middleName": "Jane",
                      "nameSuffix": "Sr."
                    },
                    "payroll": {
                      "payrollId": "1234567890",
                      "payrollType": "Full-Time",
                      "payrollGroup": "001",
                      "weeklyHours": "48"
                    },
                    "personal": {
                      "dateOfBirth": "1990-01-01",
                      "gender": "Male",
                      "smoker": false,
                      "socialSecurityNumber": "447119876",
                      "maritalStatus": "M"
                    },
                    "contactNumber": {
                      "phone": [
                        {
                          "type": "Home",
                          "number": "123-456-5890"
                        },
                        {
                          "type": "Work",
                          "number": "987-654-2210"
                        }
                      ]
                    },
                    "validFrom": "2024-07-01",
                    "validTo": "9999-12-31"
                  }
                ],
                "planTypes": [
                  {
                    "enrollmentRevisions": [
                      {
                        "enrollment": {
                          "current": {
                            "carrierName": "NO_CARRIER_FOUND",
                            "cobraIn": false,
                            "use1CGAPI": false,
                            "costs": {
                              "employerContribution": {
                                "preTax": {
                                  "payPeriod": 0.0,
                                  "annual": 0.0,
                                  "monthly": 0.0
                                }
                              },
                              "participantAmount": {
                                "postTax": {
                                  "payPeriod": 0.0,
                                  "annual": 0.0,
                                  "monthly": 0.0
                                }
                              }
                            },
                            "courtOrdered": false,
                            "coverageAmountStartDate": "2024-07-01",
                            "electedCoverageAmount": 0.0,
                            "electedGoalAmount": 0.0,
                            "electedOptionId": "ADDWAIVE",
                            "electedOptionName": "ADDWAIVE",
                            "electionSource": "3",
                            "enrollingEvent": "New Hire",
                            "enrollingEventDate": "2024-07-01",
                            "goalAmountEnd": "2024-12-31",
                            "goalAMountStart": "2024-07-01",
                            "hippaIn": false,
                            "inforceCoverageAmount": 0.0,
                            "inforceGoalAmount": 0.0,
                            "inforceOptionId": "ADDWAIVE",
                            "inforceOptionName": "ADDWAIVE",
                            "optionEffectiveStart": "2024-07-01",
                            "isCovered": false
                          }
                        },
                        "participantHash": "c112hsvdjkcs231",
                        "validFrom": "2024-07-01",
                        "validTo": "9999-12-31",
                        "ignoreCarrierProcessing": false
                      }
                    ],
                    "standardBenefitArea": "ADD",
                    "standardBenefitAreaName": "Optional AD&D"
                  },
                  {
                    "enrollmentRevisions": [
                      {
                        "enrollment": {
                          "current": {
                            "carrierName": "Aetna",
                            "cobraIn": false,
                            "use1CGAPI": true,
                            "costs": {
                              "creditAmount": {
                                "preTax": {
                                  "payPeriod": 0.0,
                                  "annual": 0.0,
                                  "monthly": 0.0
                                }
                              },
                              "employerContribution": {
                                "preTax": {
                                  "payPeriod": 3.85,
                                  "annual": 100.0,
                                  "monthly": 8.33
                                }
                              },
                              "participantAmount": {
                                "preTax": {
                                  "payPeriod": 57000.69,
                                  "annual": 1500.0,
                                  "monthly": 125.0
                                }
                              },
                              "surchargeAmount": {
                                "preTax": {
                                  "payPeriod": 0.0,
                                  "annual": 0.0,
                                  "monthly": 0.0
                                }
                              }
                            },
                            "courtOrdered": false,
                            "coverageLevel": "PPONLY",
                            "coverageAmountStartDate": "2024-07-01",
                            "electedOptionId": "AetnaPPO",
                            "electedOptionName": "AetnaPPO",
                            "electionSource": "1",
                            "enrollingEvent": "New Hire",
                            "enrollingEventDate": "2024-07-01",
                            "hippaIn": false,
                            "inforceOptionId": "AetnaPPO",
                            "inforceOptionName": "AetnaPPO",
                            "optionEffectiveStart": "2024-07-01",
                            "originalCoverageDate": "2024-07-01",
                            "isCovered": false
                          }
                        },
                        "participantHash": "c1127jhgjkcs231",
                        "validFrom": "2024-07-01",
                        "validTo": "2024-06-30",
                        "ignoreCarrierProcessing": false
                      }
                    ],
                    "standardBenefitArea": "MEDICAL",
                    "standardBenefitAreaName": "MedicalPri"
                  }
                ],
                "testEnvironment": true
              }
            }
            """;

    private Map<String, Object> rootMap;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() throws Exception {
        rootMap = objectMapper.readValue(SAMPLE_PAYLOAD, new TypeReference<>() {});
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
    public void testParticipantSSNWithJsonErrorDetails() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("root", rootMap);

        String ruleSpel =
                """
                    #root['payload']['participantRevisions']
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
        context.setVariable("root", rootMap);

        String spel =
                """
                    #root['payload']['participantRevisions']
                        .![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                            'ssn': #this['personal']['socialSecurityNumber']}]
                        .?[#this['ssn'] == null or !#this['ssn'].matches('^(\\d{3}-\\d{2}-\\d{4}|\\d{9})$')]
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
    public void testAllDependentSSNNotNullOrEmpty() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Select dependents with null or empty SSN
        String expression =
                "#root['payload']['dependents'].?[#this['personal']['socialSecurityNumber'] == null or #this['personal']['socialSecurityNumber'] == '']";
        List<?> invalidDependents =
                parser.parseExpression(expression).getValue(context, List.class);

        assertTrue(
                invalidDependents.isEmpty(),
                "All dependents should have a non-null and non-empty SSN");
    }

    @Test
    public void testDependentSSNFormatUsingPattern() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        String expression =
                "#root['payload']['dependents'].?[ !T(java.util.regex.Pattern).matches('[0-9]{3}-[0-9]{2}-[0-9]{4}|[0-9]{9}', #this['personal']['socialSecurityNumber']) ].![#this['personal']['socialSecurityNumber']]";

        List<String> failedSSNValues =
                parser.parseExpression(expression).getValue(context, List.class);

        System.out.println("Failed SSN values: " + failedSSNValues);
        assertTrue(
                failedSSNValues.isEmpty(),
                "Dependent SSN format violations found: " + failedSSNValues);
    }

    @Test
    public void testNoDuplicateDependentSSNBetter() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        String expression =
                "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']]";
        List<String> ssnList = parser.parseExpression(expression).getValue(context, List.class);

        List<String> duplicates =
                ssnList.stream()
                        .filter(s -> Collections.frequency(ssnList, s) > 1)
                        .distinct()
                        .collect(Collectors.toList());

        assertTrue(duplicates.isEmpty(), "Duplicate dependent SSN values found: " + duplicates);
    }

    @Test
    public void testDuplicateDependentSSNUsingSingleSpEL() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Single SpEL expression to get a list of unique duplicate dependent SSNs.
        String expression =
                "new java.util.ArrayList(new java.util.HashSet("
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']]."
                        + "?[T(java.util.Collections).frequency("
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']], #this) > 1]"
                        + "))";

        List<String> duplicates = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Duplicate dependent SSNs (from single SpEL): " + duplicates);

        assertTrue(duplicates.isEmpty(), "Duplicate dependent SSN values found: " + duplicates);
    }

    @Test
    public void testDuplicateSSNs_ParticipantAndDependent_WithNormalization() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);
        context.setVariable("root", rootMap);

        String spel =
                "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "#root['payload']['participantRevisions']"
                        + ".![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}]"
                        + ".?[T(org.apache.commons.collections4.CollectionUtils).intersection("
                        + "#root['payload']['participantRevisions'].![#this['personal']['socialSecurityNumber']?.replace('-', '')], "
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']?.replace('-', '')]"
                        + ").contains(#this['ssn']?.replace('-', ''))],"
                        + "#root['payload']['dependents']"
                        + ".![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}]"
                        + ".?[T(org.apache.commons.collections4.CollectionUtils).intersection("
                        + "#root['payload']['participantRevisions'].![#this['personal']['socialSecurityNumber']?.replace('-', '')], "
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']?.replace('-', '')]"
                        + ").contains(#this['ssn']?.replace('-', ''))])";

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
    public void testDuplicateBetweenParticipantAndDependentSSNUsingSpEL() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Single SpEL expression that:
        // 1. Projects participant SSNs from participantRevisions.
        // 2. Projects dependent SSNs from dependents.
        // 3. Uses CollectionUtils.intersection to compute duplicates.
        String expression =
                "T(org.apache.commons.collections4.CollectionUtils).intersection("
                        + "#root['payload']['participantRevisions'].![#this['personal']['socialSecurityNumber']], "
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']]"
                        + ")";

        List<String> duplicates = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Duplicate SSNs between participant and dependent: " + duplicates);

        // The test asserts that there are no duplicates. If duplicates exist, the test will fail
        // and display them.
        assertTrue(
                duplicates.isEmpty(),
                "Duplicate SSNs found between participant and dependent: " + duplicates);
    }

    @Test
    public void testDuplicateBetweenAllGroupsSSNUsingSingleSpEL() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // The expression does the following:
        // 1. Combines beneficiary, dependent, and participant SSN lists via union.
        // 2. Wraps that combined list in a HashSet to eliminate redundant duplicate values.
        // 3. Selects those SSN values for which the frequency in the full union is greater than 1.
        String expression =
                "new java.util.ArrayList(new java.util.HashSet("
                        + "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "#root['payload']['beneficiaries'].![#this['person']['socialSecurityNumber']], "
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']]"
                        + "), "
                        + "#root['payload']['participantRevisions'].![#this['personal']['socialSecurityNumber']]"
                        + ")"
                        + ")).?[T(java.util.Collections).frequency("
                        + "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "#root['payload']['beneficiaries'].![#this['person']['socialSecurityNumber']], "
                        + "#root['payload']['dependents'].![#this['personal']['socialSecurityNumber']]"
                        + "), "
                        + "#root['payload']['participantRevisions'].![#this['personal']['socialSecurityNumber']]"
                        + "), #this) > 1]";

        List<String> duplicates = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println(
                "Duplicate SSNs across beneficiaries, dependents and participant: " + duplicates);
        assertTrue(duplicates.isEmpty(), "Duplicate SSN values found across groups: " + duplicates);
    }

    @Test
    public void testAllBeneficiarySSNNotNullOrEmpty() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Select beneficiaries with null or empty SSN
        String expression =
                "#root['payload']['beneficiaries'].?[#this['person']['socialSecurityNumber'] == null or #this['person']['socialSecurityNumber'] == '']";
        List<?> invalidBeneficiaries =
                parser.parseExpression(expression).getValue(context, List.class);

        assertTrue(
                invalidBeneficiaries.isEmpty(),
                "All beneficiaries should have a non-null and non-empty SSN");
    }

    @Test
    public void testBeneficiarySSNFormatUsingPattern() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Select beneficiaries whose SSN does not match the valid formats (123-45-6789 or
        // 123456789)
        String expression =
                "#root['payload']['beneficiaries'].?[ !T(java.util.regex.Pattern).matches('[0-9]{3}-[0-9]{2}-[0-9]{4}|[0-9]{9}', #this['person']['socialSecurityNumber']) ].![#this['person']['socialSecurityNumber']]";
        List<String> failedSSNValues =
                parser.parseExpression(expression).getValue(context, List.class);

        System.out.println("Failed Beneficiary SSN values: " + failedSSNValues);
        assertTrue(
                failedSSNValues.isEmpty(),
                "Beneficiary SSN format violations found: " + failedSSNValues);
    }

    @Test
    public void testDuplicateBeneficiarySSNUsingNormalizedSpEL() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // This expression projects beneficiary SSNs, removes hyphens via replaceAll,
        // then selects those that occur more than once (using Collections.frequency),
        // wraps them in a HashSet (to ensure uniqueness), and converts to an ArrayList.
        String expression =
                "new java.util.ArrayList(new java.util.HashSet("
                        + "#root['payload']['beneficiaries'].![#this['person']['socialSecurityNumber'].replaceAll('-', '')]."
                        + "?[T(java.util.Collections).frequency("
                        + "#root['payload']['beneficiaries'].![#this['person']['socialSecurityNumber'].replaceAll('-', '')], #this) > 1]"
                        + "))";

        List<String> duplicates = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Duplicate Beneficiary SSNs (normalized): " + duplicates);

        assertTrue(
                duplicates.isEmpty(),
                "Duplicate beneficiary SSN values found (normalized): " + duplicates);
    }

    @Test
    public void testDuplicateBeneficiarySSNWithNamesUsingPureSpEL() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // This expression does the following:
        // 1. Projects each beneficiary to a map with keys "ssn" and "name".
        //    The "ssn" is the beneficiary's socialSecurityNumber with hyphens removed.
        // 2. Selects only those maps where the frequency of the normalized SSN (from all
        // beneficiaries)
        //    is greater than 1.
        // 3. Wraps the result in a HashSet to remove duplicates and converts it to an ArrayList.
        String expression =
                "new java.util.ArrayList(new java.util.HashSet("
                        + "#root['payload']['beneficiaries'].!["
                        + "{'ssn': #this['person']['socialSecurityNumber'].replaceAll('-', ''), "
                        + "'name': (#this['name']['firstName'] + ' ' + #this['name']['lastName'])}"
                        + "].?[T(java.util.Collections).frequency("
                        + "#root['payload']['beneficiaries'].![#this['person']['socialSecurityNumber'].replaceAll('-', '')], "
                        + "#this['ssn']) > 1]"
                        + "))";

        List<?> duplicates = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Duplicate Beneficiary SSNs with Names (normalized): " + duplicates);

        // The test asserts that there are no duplicate entries.
        assertTrue(
                duplicates.isEmpty(),
                "Duplicate beneficiary SSN values (normalized) found with names: " + duplicates);
    }

    @Test
    public void testPlanTypesValidFromEarlierThanValidTo() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // The expression:
        // 1. For each planType, it projects a list of booleans for its enrollmentRevisions checking
        // if validFrom < validTo.
        // 2. The inner 'contains(false)' returns true if any enrollmentRevision fails.
        // 3. The negation then yields true if all enrollmentRevisions are valid for that planType.
        // 4. Finally, it checks that none of the planTypes have failed (i.e. the outer list does
        // not contain false).
        String expression =
                "!#root['payload']['planTypes'].![ "
                        + "!(#this['enrollmentRevisions'].![ "
                        + "T(java.time.LocalDate).parse(#this['validFrom']) < T(java.time.LocalDate).parse(#this['validTo']) "
                        + "].contains(false)) "
                        + "].contains(false)";

        Boolean allValid = parser.parseExpression(expression).getValue(context, Boolean.class);
        assertTrue(
                allValid,
                "One or more planType enrollment revisions have validFrom not earlier than validTo");
    }

    public static List<Object> flatten(List<?> listOfLists) {
        return listOfLists.stream()
                .filter(item -> item instanceof List)
                .map(item -> (List<?>) item)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Test
    public void testPlanTypesValidFromEarlierThanValidToWithFailedValues() throws Exception {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Register the flatten helper so it can be invoked from the SpEL expression.
        context.registerFunction(
                "flatten", SpelTests.class.getDeclaredMethod("flatten", List.class));

        // Updated expression using bracket notation to access 'validFrom' and 'validTo' from the
        // map.
        String expression =
                "#flatten(#root['payload']['planTypes'].![#this['enrollmentRevisions']."
                        + "?[T(java.time.LocalDate).parse(#this['validFrom']) >= T(java.time.LocalDate).parse(#this['validTo'])]."
                        + "![{'validFrom': #this['validFrom'], 'validTo': #this['validTo']}]])";

        List<?> failedValues = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Failed enrollment revisions: " + failedValues);

        assertTrue(
                failedValues.isEmpty(),
                "One or more planType enrollment revisions have validFrom not earlier than validTo: "
                        + failedValues);
    }

    @Test
    public void testChildDependentAgeNotGreaterThanParticipantAge() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // This expression selects from the dependents array those entries where:
        // - The dependent's relationship is "Child", and
        // - The dependent's dateOfBirth (parsed as LocalDate) is NOT after the participant's
        // dateOfBirth (from the first participantRevision).
        // In other words, it selects child dependents whose birth date is on or before the
        // participant's birth date.
        String expression =
                "#root['payload']['dependents']."
                        + "?[#this['personal']['relationship'] == 'Child' "
                        + "and T(java.time.LocalDate).parse(#this['personal']['dateOfBirth']) <= T(java.time.LocalDate).parse("
                        + "#root['payload']['participantRevisions'][0]['personal']['dateOfBirth'])]."
                        + "![{'firstName': #this['name']['firstName'], 'lastName': #this['name']['lastName'], 'dateOfBirth': #this['personal']['dateOfBirth']}]";

        List<?> failingDependents =
                parser.parseExpression(expression).getValue(context, List.class);
        System.out.println(
                "Child dependents with age not less than participant's age: " + failingDependents);

        // The test asserts that the list of failing dependents is empty.
        assertTrue(
                failingDependents.isEmpty(),
                "Found child dependents with age not less than participant age: "
                        + failingDependents);
    }

    @Test
    public void testGenderValuesAndNamesAreMOrF() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        String expression =
                "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "T(org.apache.commons.collections4.CollectionUtils).union("
                        + "#root['payload']['participantRevisions'].![{'name': (#this['name']['firstName'] + ' ' + #this['name']['lastName']), 'gender': #this['personal']['gender']}], "
                        + "#root['payload']['dependents'].![{'name': (#this['name']['firstName'] + ' ' + #this['name']['lastName']), 'gender': #this['personal']['gender']}]"
                        + "), "
                        + "#root['payload']['beneficiaries'].![{'name': (#this['name']['firstName'] + ' ' + #this['name']['lastName']), 'gender': #this['person']['gender']}]"
                        + ").?[!(#this['gender'] == 'M' or #this['gender'] == 'F')]";

        List<?> invalidEntries = parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Invalid gender entries: " + invalidEntries);

        assertTrue(
                invalidEntries.isEmpty(),
                "Invalid gender values found. The following person(s) have invalid gender: "
                        + invalidEntries
                        + ". Allowed gender values are only 'M' or 'F'.");
    }

    @Test
    public void testPreTaxViolationCount1() {
        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

        // Corrected expression with proper syntax
        String expression =
                "#{payload.planTypes.*.enrollmentRevisions.*.enrollment.current.participantAmount.preTax.?[this != null && annual <= payPeriod].isEmpty()}";

        // Evaluate the expression
        List<Integer> violationCounts =
                parser.parseExpression(expression).getValue(context, List.class);
        System.out.println("Violation counts per plan type: " + violationCounts);

        // Check if any violations exist
        boolean hasViolations = violationCounts.stream().anyMatch(count -> count > 0);
        assertTrue(
                hasViolations,
                "Expected to find cases where preTax annual ≤ payPeriod or monthly, but none were found. "
                        + "Violation counts: "
                        + violationCounts);
    }
}

-- Insert data into system table
INSERT INTO bcpm_primary_schema.system (system_id, name_tx)
VALUES ('HOS', 'HWSE Outbound Service'),
       ('CGR', 'Carrier Gateway Reporting Service');

-- Insert data into process table
INSERT INTO bcpm_primary_schema.process (process_id, name_tx, system_id)
VALUES ('publish_to_carrier', 'Publish HWSE Enrollments and Demographic Canonical Data', 'CGR'),
       ('hwse_event_processing', 'Process HWSE events processing data', 'HOS');

-- Insert data into tenant_registry table
INSERT INTO bcpm_primary_schema.tenant_registry (tenant_id, schema_tx, database_tx, connection_tx, real_client, test_data)
VALUES ('TENANT1', 'bcpm_client_schema', 'cpmclient1_local',
'jdbc:postgresql://localhost:5444/cpmclient1_local?currentSchema=bcpm_client_schema', TRUE, FALSE),
       ('TENANT2', 'bcpm_client_schema', 'cpmclient2_local',
       'jdbc:postgresql://localhost:5445/cpmclient2_local?currentSchema=bcpm_client_schema', TRUE, FALSE);

-- Insert data into client table
INSERT INTO bcpm_primary_schema.client (client_id, tenant_id, name_tx, oid)
VALUES ('11111111-1111-1111-1111-111111111111', 'TENANT1', 'NIR2CONFIG', '98BE5B782270036B'),
       ('22222222-2222-2222-2222-222222222222', 'TENANT2', 'NASDOMAIN', 'G3WZV1HV3S228QJG1');

-- Insert data into process_client_assoc table
INSERT INTO bcpm_primary_schema.process_client_assoc (process_id, client_id)
VALUES ('publish_to_carrier', '11111111-1111-1111-1111-111111111111'),
       ('publish_to_carrier', '22222222-2222-2222-2222-222222222222'),
       ('hwse_event_processing', '11111111-1111-1111-1111-111111111111'),
       ('hwse_event_processing', '22222222-2222-2222-2222-222222222222');

-- Insert data into process_step table
INSERT INTO bcpm_primary_schema.process_step (process_step_id, name_tx, process_id, async_in)
VALUES
    ('consume_kafka_from_gg', 'Consume Kafka Notification', 'hwse_event_processing', FALSE),
    ('build_event_data', 'Build Payload', 'hwse_event_processing', FALSE),
    ('publish_hwse_payload', 'Publish HUSE Outbound', 'hwse_event_processing', FALSE),
    ('consume_kafka_notification_cgr', 'Consume Kafka Notification from outbound', 'publish_to_carrier', FALSE),
    ('split_payload_by_carrier', 'Split Payload by Carrier', 'publish_to_carrier', FALSE),
    ('default_data_mapping', 'Default Data Mapping', 'publish_to_carrier', FALSE),
    ('standard_lookup_mapping', 'Standard Lookup Mapping', 'publish_to_carrier', FALSE),
    ('standard_derived_transform', 'Standard Derived Transform', 'publish_to_carrier', FALSE),
    ('carrier_client_transform', 'Carrier Client Transform', 'publish_to_carrier', FALSE),
    ('carrier_client_transform_send', 'Carrier Client Transform Send', 'publish_to_carrier', FALSE),
    ('carrier_client_transform_receive', 'Carrier Client Transform Receive', 'publish_to_carrier', FALSE),
    ('publish_canonical_to_1cg', 'Publish Canonical to 1CG', 'publish_to_carrier', FALSE),
    ('receive_error_feedback_from_1cg', 'Receive Error Feedback from 1CG', 'publish_to_carrier', FALSE),
    ('receive_carrier_send_status_from_1cg', 'Receive Carrier Send Status from 1CG', 'publish_to_carrier', FALSE),
    ('receive_carrier_acknowledgement', 'Receive Carrier Acknowledgement', 'publish_to_carrier', FALSE);

-- Insert data into client_step_override table
INSERT INTO bcpm_primary_schema.client_step_override (client_id, override_step_id, overriden_step_id,pause_in, skip_in)
VALUES ('11111111-1111-1111-1111-111111111111', 'standard_lookup_mapping', 'default_data_mapping', FALSE, FALSE);

-- Insert data into attribute table
INSERT INTO bcpm_primary_schema.attribute (attribute_id, collection_in)
VALUES ('SSN', TRUE),
       ('GENDER', TRUE),
       ('DOB', TRUE),
       ('VALID_FROM_VALID_TO', TRUE),
       ('PRICE_COMPARE', TRUE);

-- Insert data into attribute_list table
-- Attribute_List table: store for each attribute a display name, the absolute JSON pointer and (if applicable) a relative pointer.
-- Here, for SSN and DOB we have two rows each: one for the participant and one for the dependents.
INSERT INTO bcpm_primary_schema.attribute_list (attribute_list_id, attribute_id, attribute_name_tx)
VALUES
  -- For SSN:
  (1, 'SSN', 'Participant Social Security Number'),
  (2, 'SSN', 'Dependent Social Security Number'),

  -- For Gender:
  (3, 'GENDER', 'Participant or Dependent Gender'),

  -- For Coverage Price, for example:
  (4, 'DOB', 'Date of Birth'),

  -- For VALID_FROM_VALID_TO:
  (5, 'VALID_FROM_VALID_TO', 'Participant or Dependent Plan Types Valid From/Valid To'),

  (6, 'PRICE_COMPARE', 'Standard Benefit Area Plan Types Price Compare (EG: preTax -> [payPeriod ≤ annual]');

-- Insert data into proc_step_attr_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_assoc (proc_step_attr_assoc_id, attribute_id, process_step_id, enabled_in)
VALUES ('assoc1', 'SSN', 'publish_hwse_payload', TRUE),
       ('assoc2', 'DOB', 'publish_hwse_payload', TRUE),
       ('assoc3', 'GENDER', 'publish_hwse_payload', TRUE),
       ('assoc4', 'VALID_FROM_VALID_TO', 'publish_hwse_payload', TRUE),
       ('assoc5', 'PRICE_COMPARE', 'publish_hwse_payload', TRUE);


-- Insert data into message table
INSERT INTO bcpm_primary_schema.message (message_id, type_tx, body_tx, eff_start_date_dt, eff_until_date_dt, enabled_in)
VALUES ('msg1', 'FAIL', 'Participant SSN is NULL: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg2', 'FAIL', 'Participant SSN is in invalid format: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg3', 'FAIL', 'One of participant or dependent SSN is duplicate: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg4', 'FAIL', 'Dependent SSN is NULL: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg5', 'FAIL', 'Dependent SSN is in invalid format: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg6', 'FAIL', 'One of dependent SSN is duplicate: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg7', 'FAIL', 'Gender values must be M, F: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg8', 'FAIL', 'Dependent Children birth date should not be greater than Participant: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg9', 'FAIL', 'Plan Type Valid To Should be Later Date than Valid From: {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg10', 'FAIL', 'For any Plan Type for Standard Benefit Area, annual >= payPeriod: {errorDetails}', '2023-01-01', '9999-12-31', TRUE);

INSERT INTO bcpm_primary_schema.rule (
    rule_id, name_tx, rule_cond_id, eff_start_date_dt, eff_until_date_dt,
    enabled_in, message_id, rule_expression
) VALUES (
    'testParticipantSsnNotNull',
    'Participant SSN should not be NULL',
    'NullCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg1',
    '#payload[''participantRevisions''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''][''socialSecurityNumber'']}].?[#this[''ssn''] == null or #this[''ssn''].isEmpty()]'
);

-- Insert data into rule table
INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
)
VALUES (
    'testParticipantSsnValidFormat',
    'Participant SSN should be in Correct Format',
    'InvalidCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg2',
    '#payload[''participantRevisions''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''][''socialSecurityNumber'']}].?[#this[''ssn''] != null and !#this[''ssn''].isEmpty() and !#this[''ssn''].matches(''^(\\d{3}-\\d{2}-\\d{4}|\\d{9})$'')]'
);


-- For testParticipantSsnNotSameAsDependentSsn
INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testParticipantSsnNotSameAsDependentSsn',
    'Participant SSN should not match any dependent SSN',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg3',
    'T(org.apache.commons.collections4.CollectionUtils).union(#payload[''participantRevisions''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''][''socialSecurityNumber'']}].?[T(org.apache.commons.collections4.CollectionUtils).intersection(#payload[''participantRevisions''].![#this[''personal''][''socialSecurityNumber''] != null ? #this[''personal''][''socialSecurityNumber''].replace(''-'', '''') : ''''], #payload[''dependents''].![#this[''personal''][''socialSecurityNumber''] != null ? #this[''personal''][''socialSecurityNumber''].replace(''-'', '''') : '''']).contains(#this[''ssn''] != null ? #this[''ssn''].replace(''-'', '''') : '''')], #payload[''dependents''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''][''socialSecurityNumber'']}].?[T(org.apache.commons.collections4.CollectionUtils).intersection(#payload[''participantRevisions''].![#this[''personal''][''socialSecurityNumber''] != null ? #this[''personal''][''socialSecurityNumber''].replace(''-'', '''') : ''''], #payload[''dependents''].![#this[''personal''][''socialSecurityNumber''] != null ? #this[''personal''][''socialSecurityNumber''].replace(''-'', '''') : '''']).contains(#this[''ssn''] != null ? #this[''ssn''].replace(''-'', '''') : '''')])'
);

-- For testDependentSsnNotNull
INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testDependentSsnNotNull',
    'Dependent SSN should not be NULL or empty',
    'NullCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg4',
    '#payload[''dependents''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''] != null ? #this[''personal''][''socialSecurityNumber''] : null}].?[#this[''ssn''] == null or #this[''ssn''] == '''']'
);

INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testDependentSSNFormat',
    'Dependent SSN should be in valid format (###-##-#### or #########)',
    'InvalidCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg5',
    '#payload[''dependents''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''][''socialSecurityNumber'']}].?[#this[''ssn''] != null and !#this[''ssn''].isEmpty() and !#this[''ssn''].matches(''^([0-9]{3}-[0-9]{2}-[0-9]{4}|[0-9]{9})$'')]'
);

INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testDuplicateDependentSSN',
    'Dependent SSNs should be unique',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg6',
    '#payload[''dependents''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''ssn'': #this[''personal''][''socialSecurityNumber'']}].?[T(java.util.Collections).frequency(#payload[''dependents''].![#this[''personal''][''socialSecurityNumber''] != null ? #this[''personal''][''socialSecurityNumber''].replace(''-'', '''') : ''''], #this[''ssn''] != null ? #this[''ssn''].replace(''-'', '''') : '''') > 1]'
);

INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testGenderValues',
    'Gender values must be M or F',
    'EnumCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg7',
    'T(org.apache.commons.collections4.CollectionUtils).union(T(org.apache.commons.collections4.CollectionUtils).union(#payload[''participantRevisions''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''gender'': #this[''personal''][''gender'']}], #payload[''dependents''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''gender'': #this[''personal''][''gender'']}]), #payload[''beneficiaries''].![{''name'': #this[''name''][''firstName''] + '' '' + #this[''name''][''lastName''], ''gender'': #this[''person''][''gender'']}]).?[!(#this[''gender''] == ''M'' or #this[''gender''] == ''F'')]'
);

INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testChildDependentAge',
    'Child dependent age must be less than participant age',
    'CompareCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg8',
    '#payload[''dependents''].?[#this[''personal''][''relationship''] == ''Child'' and T(java.time.LocalDate).parse(#this[''personal''][''dateOfBirth'']) <= T(java.time.LocalDate).parse(#payload[''participantRevisions''][0][''personal''][''dateOfBirth''])].![{''firstName'': #this[''name''][''firstName''], ''lastName'': #this[''name''][''lastName''], ''dateOfBirth'': #this[''personal''][''dateOfBirth'']}]'
);

INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testPlanTypeDateValidation',
    'PlanType validFrom must be earlier than validTo',
    'CompareCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg9',
    '#payload[''planTypes''].![#this[''enrollmentRevisions''].![#this].?[T(java.time.LocalDate).parse(#this[''validFrom'']).isAfter(T(java.time.LocalDate).parse(#this[''validTo''])) or T(java.time.LocalDate).parse(#this[''validFrom'']).isEqual(T(java.time.LocalDate).parse(#this[''validTo'']))].![{''validFrom'': #this[''validFrom''], ''validTo'': #this[''validTo'']}]].![#this].?[!#this.empty]'
);


INSERT INTO bcpm_primary_schema.rule (
    rule_id,
    name_tx,
    rule_cond_id,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in,
    message_id,
    rule_expression
) VALUES (
    'testAnnualGreaterThanPayPeriodForAllStandardBenefitAreaTypes',
    'Annual participant amount must be greater than pay period amount when preTax exists',
    'condX',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg10',
    '#payload[''planTypes''].![#this[''enrollmentRevisions''][0][''enrollment''][''current''][''costs''][''participantAmount''][''preTax''] != null ? {''annual'': #this[''enrollmentRevisions''][0][''enrollment''][''current''][''costs''][''participantAmount''][''preTax''][''annual''], ''payPeriod'': #this[''enrollmentRevisions''][0][''enrollment''][''current''][''costs''][''participantAmount''][''preTax''][''payPeriod''], ''benefitArea'': #this[''standardBenefitArea'']} : null].?[#this != null and #this[''annual''] <= #this[''payPeriod'']]'
);


-- Insert data into proc_step_attr_rule_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_assoc (proc_step_attr_rule_assoc_id, proc_step_attr_assoc_id, rule_id, enabled_in)
VALUES ('assoc_rule1', 'assoc1', 'testParticipantSsnNotNull', TRUE),
       ('assoc_rule2', 'assoc1', 'testParticipantSsnValidFormat', TRUE),
       ('assoc_rule3', 'assoc1', 'testParticipantSsnNotSameAsDependentSsn', TRUE),
       ('assoc_rule4', 'assoc1', 'testDependentSsnNotNull', TRUE),
       ('assoc_rule5', 'assoc1', 'testDependentSSNFormat', TRUE),
       ('assoc_rule6', 'assoc1', 'testDuplicateDependentSSN', TRUE),
       ('assoc_rule7', 'assoc3', 'testGenderValues', TRUE),
       ('assoc_rule8', 'assoc2', 'testChildDependentAge', TRUE),
       ('assoc_rule9', 'assoc4', 'testPlanTypeDateValidation', TRUE),
       ('assoc_rule10', 'assoc5', 'testAnnualGreaterThanPayPeriodForAllStandardBenefitAreaTypes', TRUE);

-- Insert data into proc_step_attr_rule_msg_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_msg_assoc (proc_step_attr_rule_assoc_id, message_id)
VALUES ('assoc_rule1', 'msg1'),
       ('assoc_rule2', 'msg2'),
       ('assoc_rule3', 'msg3'),
       ('assoc_rule4', 'msg4'),
       ('assoc_rule5', 'msg5'),
       ('assoc_rule6', 'msg6'),
       ('assoc_rule7', 'msg7'),
       ('assoc_rule8', 'msg8'),
       ('assoc_rule9', 'msg9'),
       ('assoc_rule10', 'msg10');

-- Insert data into remediation_policy table
INSERT INTO bcpm_primary_schema.remediation_policy (remediation_policy_id, name_tx, policy)
VALUES
    ('policy1', 'SSN Remediation',
        '{
            "steps": [
                {
                    "userMessage": "Notify the ADP practitioner to correct Participant SSN in the SOR.",
                    "callback": false
                }
            ]
        }'
    ),
    ('policy2', 'Failed due to unknown Error in CGR',
        '{
            "steps": [
                {
                    "userMessage": "Notify the ADP practitioner to correct Participant SSN in the SOR.",
                    "callback": true,
                    "callbackUrl": "http://localhost:8080/callback"
                }
            ]
        }'
    );
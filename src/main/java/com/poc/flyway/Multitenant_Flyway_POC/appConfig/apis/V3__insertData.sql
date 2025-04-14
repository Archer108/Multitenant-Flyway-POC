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
INSERT INTO bcpm_primary_schema.process_step (process_step_id, name_tx, process_id, async_in, step_order)
VALUES
    ('consume_kafka_from_gg', 'Consume Kafka Notification', 'hwse_event_processing', FALSE, 1),
    ('build_event_data', 'Build Payload', 'hwse_event_processing', FALSE, 2),
    ('publish_hwse_payload', 'Publish HUSE Outbound', 'hwse_event_processing', FALSE, 3),
    ('consume_kafka_notification_cgr', 'Consume Kafka Notification from outbound', 'publish_to_carrier', FALSE, 1),
    ('split_payload_by_carrier', 'Split Payload by Carrier', 'publish_to_carrier', FALSE, 2),
    ('default_data_mapping', 'Default Data Mapping', 'publish_to_carrier', FALSE, 3),
    ('standard_lookup_mapping', 'Standard Lookup Mapping', 'publish_to_carrier', FALSE, 4),
    ('standard_derived_transform', 'Standard Derived Transform', 'publish_to_carrier', FALSE, 5),
    ('carrier_client_transform', 'Carrier Client Transform', 'publish_to_carrier', FALSE, 6),
    ('carrier_client_transform_send', 'Carrier Client Transform Send', 'publish_to_carrier', FALSE, 7),
    ('carrier_client_transform_receive', 'Carrier Client Transform Receive', 'publish_to_carrier', FALSE, 8),
    ('publish_canonical_to_1cg', 'Publish Canonical to 1CG', 'publish_to_carrier', FALSE, 9),
    ('receive_error_feedback_from_1cg', 'Receive Error Feedback from 1CG', 'publish_to_carrier', FALSE, 10),
    ('receive_carrier_send_status_from_1cg', 'Receive Carrier Send Status from 1CG', 'publish_to_carrier', FALSE, 11),
    ('receive_carrier_acknowledgement', 'Receive Carrier Acknowledgement', 'publish_to_carrier', FALSE, 12);

-- Insert data into client_step_override table
INSERT INTO bcpm_primary_schema.client_step_override (client_id, override_step_id, overriden_step_id,pause_in, skip_in)
VALUES ('11111111-1111-1111-1111-111111111111', 'standard_lookup_mapping', 'default_data_mapping', FALSE, FALSE);

-- Insert data into attribute table
INSERT INTO bcpm_primary_schema.attribute (attribute_id, collection_in)
VALUES ('SSN', TRUE),
        ('UUID', TRUE),
        ('DOB', TRUE);

-- Insert data into attribute_list table
-- Attribute_List table: store for each attribute a display name, the absolute JSON pointer and (if applicable) a relative pointer.
-- Here, for SSN and DOB we have two rows each: one for the participant and one for the dependents.
INSERT INTO bcpm_primary_schema.attribute_list (attribute_list_id, attribute_id, attribute_name_tx)
VALUES
  -- For SSN:
--  (1, 'SSN', 'Participant Social Security Number'),
 -- (2, 'SSN', 'Dependent Social Security Number'),
  -- (3, 'SSN', 'Beneficiary Social Security Number'),

  -- For Gender:
--  (4, 'GENDER', 'Participant or Dependent Gender'),
--
--  -- For Coverage Price, for example:
  (5, 'DOB', 'Date of Birth');
--
--  -- For VALID_FROM_VALID_TO:
--  (6, 'VALID_FROM_VALID_TO', 'Participant or Dependent Plan Types Valid From/Valid To'),
--
--  (7, 'PRICE_COMPARE', 'Standard Benefit Area Plan Types Price Compare (EG: preTax -> [payPeriod ≤ annual]');

-- Insert data into proc_step_attr_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_assoc (proc_step_attr_assoc_id, attribute_id, process_step_id, enabled_in)
VALUES ('assoc1', 'SSN', 'build_event_data', TRUE),
       ('assoc2', 'UUID', 'publish_canonical_to_1cg', TRUE);


-- Insert data into message table
INSERT INTO bcpm_primary_schema.message (message_id, type_tx, body_tx, eff_start_date_dt, eff_until_date_dt, enabled_in)
VALUES ('msg1', 'FAIL', '{attributeName} SSN is NULL. Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg2', 'FAIL', '{attributeName} SSN should be in valid format (###-##-#### or #########). Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg3', 'FAIL', 'Ensure no duplicate SSNs exists across entities: Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg4', 'FAIL', 'RULE_DUPLICATE_SSN_PARTICIPANTS_DEPENDENTS: Ensure no duplicate SSNs exists across entities: Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg5', 'FAIL', 'RULE_DUPLICATE_SSN_PARTICIPANTS_BENEFICIARIES: Ensure no duplicate SSNs exists across entities: Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msg6', 'FAIL', 'RULE_DUPLICATE_SSN_DEPENDENTS_BENEFICIARIES: Ensure no duplicate SSNs exists across entities: Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msgCombined', 'FAIL', 'RULE_ALL_DUPLICATE_SSN: Ensure no duplicate SSNs exists across entities: Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE),
       ('msgUUID', 'FAIL', 'RULE_UUID_REQUIRED_AND_VALID_FORMAT: UUID is NULL or in invalid format: Error Details -> {errorDetails}', '2023-01-01', '9999-12-31', TRUE);

INSERT INTO bcpm_primary_schema.rule (
    rule_id, name_tx, rule_cond_id, eff_start_date_dt, eff_until_date_dt,
    enabled_in, message_id, rule_expression
) VALUES (
    'RULE_SSN_REQUIRED',
    'Participant SSN should not be NULL',
    'NullCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg1',
    $rule$
#payload[{attributePath}].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': (#this['person'] != null ? #this['person']['socialSecurityNumber'] : (#this['personal'] != null ? #this['personal']['socialSecurityNumber'] : null))}].?[#this['ssn'] == null or #this['ssn'].trim().isEmpty()]
$rule$
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
)
VALUES (
    'RULE_SSN_FORMAT_VALIDATION',
    'Participant SSN should be in Correct Format',
    'InvalidCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg2',
    $rule$
#payload[{attributePath}].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': (#this['person'] != null ? #this['person']['socialSecurityNumber'] : (#this['personal'] != null ? #this['personal']['socialSecurityNumber'] : null))}].?[#this['ssn'] != null and !#this['ssn'].trim().isEmpty() and !#this['ssn'].matches('^(\\d{3}-\\d{2}-\\d{4}|\\d{9})$')]
$rule$
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
)
VALUES (
    'RULE_DUPLICATE_DEPENDENT_SSN',
    'Dependent SSNs must be unique',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg3',
    $rule$
#payload['dependents'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                           'ssn': #this['personal']['socialSecurityNumber'],
                           'source': 'dependents'}].?[
    (#this['ssn'] != null and !#this['ssn'].trim().isEmpty() and
     T(java.util.Collections).frequency(
         #payload['dependents'].![#this['personal']['socialSecurityNumber'] != null
             ? #this['personal']['socialSecurityNumber'].replace('-', '')
             : ''
         ],
         #this['ssn'] != null ? #this['ssn'].replace('-', '') : ''
     ) > 1)
]
$rule$
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
)
VALUES (
    'RULE_PARTICIPANT_DEPENDENT_SSN',
    'Dependent SSNs must be unique',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg3',
    $rule$
#payload['participantRevisions'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                                    'ssn': #this['personal']['socialSecurityNumber'],
                                    'source': 'participantRevisions'}].?[
    (#this['ssn'] != null and !#this['ssn'].trim().isEmpty() and
     T(java.util.Collections).frequency(
         #payload['participantRevisions'].![#this['personal']['socialSecurityNumber'] != null
               ? #this['personal']['socialSecurityNumber'].replace('-', '')
               : ''
         ],
         #this['ssn'] != null ? #this['ssn'].replace('-', '') : ''
     ) > 1)
]
$rule$
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
)
VALUES (
    'RULE_BENEFICIARIES_DEPENDENT_SSN',
    'Dependent SSNs must be unique',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg3',
    $rule$
#payload['beneficiaries'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
                              'ssn': #this['person']['socialSecurityNumber'],
                              'source': 'beneficiaries'}].?[
    (#this['ssn'] != null and !#this['ssn'].trim().isEmpty() and
     T(java.util.Collections).frequency(
         #payload['beneficiaries'].![#this['person']['socialSecurityNumber'] != null
              ? #this['person']['socialSecurityNumber'].replace('-', '')
              : ''
         ],
         #this['ssn'] != null ? #this['ssn'].replace('-', '') : ''
     ) > 1)
]
$rule$
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
)
VALUES (
    'RULE_DUPLICATE_SSN_PARTICIPANTS_DEPENDENTS',
    'Duplicate SSNs across participantRevisions and dependents',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg4',
    $rule$
T(org.apache.commons.collections4.CollectionUtils).union(
  #payload['participantRevisions'].![
    {
      'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
      'ssn': #this['personal']['socialSecurityNumber'],
      'source': 'participantRevisions'
    }
  ].?[
    (#this['ssn'] != null and !#this['ssn'].trim().isEmpty() and
     T(org.apache.commons.collections4.CollectionUtils).intersection(
       #payload['participantRevisions'].![
         #this['personal']['socialSecurityNumber'] != null
           ? #this['personal']['socialSecurityNumber'].replace('-', '')
           : ''
       ],
       #payload['dependents'].![
         #this['personal']['socialSecurityNumber'] != null
           ? #this['personal']['socialSecurityNumber'].replace('-', '')
           : ''
       ]
     ).contains(#this['ssn'].replace('-', ''))
    )
  ],
  #payload['dependents'].![
    {
      'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
      'ssn': #this['personal']['socialSecurityNumber'],
      'source': 'dependents'
    }
  ].?[
    (#this['ssn'] != null and !#this['ssn'].trim().isEmpty() and
     T(org.apache.commons.collections4.CollectionUtils).intersection(
       #payload['participantRevisions'].![
         #this['personal']['socialSecurityNumber'] != null
           ? #this['personal']['socialSecurityNumber'].replace('-', '')
           : ''
       ],
       #payload['dependents'].![
         #this['personal']['socialSecurityNumber'] != null
           ? #this['personal']['socialSecurityNumber'].replace('-', '')
           : ''
       ]
     ).contains(#this['ssn'].replace('-', ''))
    )
  ]
)
$rule$
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
)
VALUES (
    'RULE_DUPLICATE_SSN_PARTICIPANTS_BENEFICIARIES',
    'Duplicate SSNs across participantRevisions and beneficiaries',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg5',
    $rule$
T(org.apache.commons.collections4.CollectionUtils).union(
  #payload['participantRevisions'].![
    {
      'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
      'ssn': #this['personal']['socialSecurityNumber'],
      'source': 'participantRevisions'
    }
  ].?[
    (
      #this['ssn'] != null and
      !#this['ssn'].trim().isEmpty() and
      T(org.apache.commons.collections4.CollectionUtils).intersection(
        #payload['participantRevisions'].![
          #this['personal']['socialSecurityNumber'] != null
            ? #this['personal']['socialSecurityNumber'].replace('-', '')
            : ''
        ],
        #payload['beneficiaries'].![
          #this['person']['socialSecurityNumber'] != null
            ? #this['person']['socialSecurityNumber'].replace('-', '')
            : ''
        ]
      ).contains(#this['ssn'].replace('-', ''))
    )
  ],
  #payload['beneficiaries'].![
    {
      'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
      'ssn': #this['person']['socialSecurityNumber'],
      'source': 'beneficiaries'
    }
  ].?[
    (
      #this['ssn'] != null and
      !#this['ssn'].trim().isEmpty() and
      T(org.apache.commons.collections4.CollectionUtils).intersection(
        #payload['participantRevisions'].![
          #this['personal']['socialSecurityNumber'] != null
            ? #this['personal']['socialSecurityNumber'].replace('-', '')
            : ''
        ],
        #payload['beneficiaries'].![
          #this['person']['socialSecurityNumber'] != null
            ? #this['person']['socialSecurityNumber'].replace('-', '')
            : ''
        ]
      ).contains(#this['ssn'].replace('-', ''))
    )
  ]
)
$rule$
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
    'RULE_DUPLICATE_SSN_DEPENDENTS_BENEFICIARIES',
    'Duplicate SSNs across dependents and beneficiaries',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msg6',
    $rule$
T(org.apache.commons.collections4.CollectionUtils).union(
  #payload['dependents'].![
    {
      'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
      'ssn': #this['personal']['socialSecurityNumber'],
      'source': 'dependents'
    }
  ].?[
    (
      #this['ssn'] != null and
      !#this['ssn'].trim().isEmpty() and
      T(org.apache.commons.collections4.CollectionUtils).intersection(
        #payload['dependents'].![
          #this['personal']['socialSecurityNumber'] != null
            ? #this['personal']['socialSecurityNumber'].replace('-', '')
            : ''
        ],
        #payload['beneficiaries'].![
          #this['person']['socialSecurityNumber'] != null
            ? #this['person']['socialSecurityNumber'].replace('-', '')
            : ''
        ]
      ).contains(#this['ssn'].replace('-', ''))
    )
  ],
  #payload['beneficiaries'].![
    {
      'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'],
      'ssn': #this['person']['socialSecurityNumber'],
      'source': 'beneficiaries'
    }
  ].?[
    (
      #this['ssn'] != null and
      !#this['ssn'].trim().isEmpty() and
      T(org.apache.commons.collections4.CollectionUtils).intersection(
        #payload['dependents'].![
          #this['personal']['socialSecurityNumber'] != null
            ? #this['personal']['socialSecurityNumber'].replace('-', '')
            : ''
        ],
        #payload['beneficiaries'].![
          #this['person']['socialSecurityNumber'] != null
            ? #this['person']['socialSecurityNumber'].replace('-', '')
            : ''
        ]
      ).contains(#this['ssn'].replace('-', ''))
    )
  ]
)
$rule$
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
    'RULE_ALL_DUPLICATE_SSN',
    'SSNs must be unique across participants, dependents, and beneficiaries',
    'DuplicateCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msgCombined',
    $rule$
T(org.apache.commons.collections4.CollectionUtils).union(
  T(org.apache.commons.collections4.CollectionUtils).union(
    #payload['participantRevisions'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}].?[
      (#payload['dependents'].![{'ssn': #this['personal']['socialSecurityNumber'] != null
         ? #this['personal']['socialSecurityNumber'].replace('-', '')
         : ''}]).contains(
         #this['ssn'] != null
         ? #this['ssn'].replace('-', '')
         : ''
      )
    ],
    #payload['participantRevisions'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}].?[
      (#payload['beneficiaries'].![{'ssn': #this['person']['socialSecurityNumber'] != null
         ? #this['person']['socialSecurityNumber'].replace('-', '')
         : ''}]).contains(
         #this['ssn'] != null
         ? #this['ssn'].replace('-', '')
         : ''
      )
    ]
  ),
  #payload['dependents'].![{'name': #this['name']['firstName'] + ' ' + #this['name']['lastName'], 'ssn': #this['personal']['socialSecurityNumber']}].?[
    (#payload['beneficiaries'].![{'ssn': #this['person']['socialSecurityNumber'] != null
       ? #this['person']['socialSecurityNumber'].replace('-', '')
       : ''}]).contains(
       #this['ssn'] != null
       ? #this['ssn'].replace('-', '')
       : ''
    )
  ]
)
$rule$
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
    'RULE_UUID_REQUIRED_AND_VALID_FORMAT',
    'GUID value is required and must be a valid UUID',
    'NullCondition',
    '2023-01-01',
    '9999-12-31',
    TRUE,
    'msgUUID',
    $rule$
(
  #payload[{attributePath}] instanceof T(java.util.Collection)
    ? #payload[{attributePath}]
    : { #payload[{attributePath}] }
).![
  { 'name': '{attributePath}', 'uuid': #this }
].?[
  #this['uuid'] == null
  or #this['uuid'].trim().isEmpty()
  or !#this['uuid'].matches('^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$')
]
$rule$
);


-- Insert data into proc_step_attr_rule_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_assoc (proc_step_attr_rule_assoc_id, proc_step_attr_assoc_id, rule_id, enabled_in)
VALUES ('assoc_rule1', 'assoc1', 'RULE_SSN_REQUIRED', TRUE),
       ('assoc_rule2', 'assoc1', 'RULE_SSN_FORMAT_VALIDATION', TRUE),
       ('assoc_rule3', 'assoc1', 'RULE_DUPLICATE_DEPENDENT_SSN', TRUE),
       ('assoc_rule4', 'assoc1', 'RULE_PARTICIPANT_DEPENDENT_SSN', TRUE),
       ('assoc_rule5', 'assoc1', 'RULE_BENEFICIARIES_DEPENDENT_SSN', TRUE),
       ('assoc_rule6', 'assoc1', 'RULE_DUPLICATE_SSN_PARTICIPANTS_DEPENDENTS', TRUE),
       ('assoc_rule7', 'assoc1', 'RULE_DUPLICATE_SSN_PARTICIPANTS_BENEFICIARIES', TRUE),
       ('assoc_rule8', 'assoc1', 'RULE_DUPLICATE_SSN_DEPENDENTS_BENEFICIARIES', TRUE),
       ('assoc_rule9', 'assoc1', 'RULE_ALL_DUPLICATE_SSN', TRUE),
       ('assoc_rule10', 'assoc2', 'RULE_UUID_REQUIRED_AND_VALID_FORMAT', TRUE);



-- Insert data into proc_step_attr_rule_msg_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_msg_assoc (proc_step_attr_rule_assoc_id, message_id)
VALUES ('assoc_rule1', 'msg1'),
       ('assoc_rule2', 'msg2'),
       ('assoc_rule3', 'msg3'),
       ('assoc_rule4', 'msg3'),
       ('assoc_rule5', 'msg3'),
       ('assoc_rule6', 'msg4'),
       ('assoc_rule7', 'msg5'),
       ('assoc_rule8', 'msg6'),
       ('assoc_rule9', 'msgCombined'),
       ('assoc_rule10', 'msgUUID');

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
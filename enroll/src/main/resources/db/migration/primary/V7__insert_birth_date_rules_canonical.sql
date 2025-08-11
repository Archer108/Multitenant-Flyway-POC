INSERT INTO bcpm_primary_schema.attribute (attribute_id, collection_in)
VALUES ('DATE_OF_BIRTH_CANONICAL', TRUE);

INSERT INTO bcpm_primary_schema.attribute_list (attribute_list_id, attribute_id, attribute_name_tx)
VALUES (9, 'DATE_OF_BIRTH_CANONICAL', 'Date of Birth');

INSERT INTO bcpm_primary_schema.proc_step_attr_assoc (proc_step_attr_assoc_id, attribute_id, process_step_id, enabled_in)
VALUES ('PSAA_DOB_PUBLISH_CANONICAL_TO_1CG', 'DATE_OF_BIRTH_CANONICAL', 'PUBLISH_CANONICAL_TO_1CG', TRUE);

INSERT INTO bcpm_primary_schema.message (message_id, type_tx, body_tx, eff_start_date_dt, eff_until_date_dt, enabled_in)
VALUES ('MSG_FAIL_DOB_NULL', 'FAIL',
    'Must not be null',
    '2023-01-01', '9999-12-31',  TRUE);

INSERT INTO bcpm_primary_schema.message (message_id, type_tx, body_tx, eff_start_date_dt, eff_until_date_dt, enabled_in)
VALUES ('MSG_FAIL_DOB_INVALID', 'FAIL',
    $msg$ {attributeName} must be a valid date. $msg$,
    '2023-01-01', '9999-12-31',  TRUE);

-- RULE: DOB should not be NULL
INSERT INTO bcpm_primary_schema.rule (rule_id, name_tx, rule_cond_id, eff_start_date_dt, eff_until_date_dt, enabled_in, message_id, rule_expression)
VALUES ('RULE_DOB_REQUIRED', 'DOB should not be NULL', 'NullCondition',
    '2023-01-01', '9999-12-31',  TRUE, 'MSG_FAIL_DOB_NULL',
    $rules$
    (
        #payload{jsonPointer} instanceof T(java.util.Collection)
        ? #payload{jsonPointer}
        : { #payload{jsonPointer}}
        ).![
        {
            'name': #this{leafPointer}['legalName']['givenName'] + ' ' + #this{leafPointer}['legalName']['familyName'],
            'dob': #this{leafPointer}['birthDate'],
            'source': '{label}'
        }
        ]
        .?[
         #this['dob'] == null or #this['dob'].trim().isEmpty()
        ]
    $rules$
);

-- RULE: DOB must be a valid date
INSERT INTO bcpm_primary_schema.rule (rule_id, name_tx, rule_cond_id, eff_start_date_dt, eff_until_date_dt, enabled_in, message_id, rule_expression)
VALUES ('RULE_DOB_FORMAT_VALIDATION', 'DOB must be a valid date', 'InvalidCondition',
    '2023-01-01', '9999-12-31',  TRUE, 'MSG_FAIL_DOB_INVALID',
    $rules$
    (
        #payload{jsonPointer} instanceof T(java.util.Collection)
        ? #payload{jsonPointer}
        : { #payload{jsonPointer}}
        ).![
        {
            'name': #this{leafPointer}['legalName']['givenName'] + ' ' + #this{leafPointer}['legalName']['familyName'],
            'dob': #this{leafPointer}['birthDate'],
            'source': '{label}'
        }
        ]
        .?[
         #this['dob'] != null and !#this['dob'].trim().isEmpty() and !#this['dob'].matches('{regex}')
          ]
    $rules$
);

-- Insert data into proc_step_attr_rule_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_assoc (proc_step_attr_rule_assoc_id, proc_step_attr_assoc_id, rule_id, enabled_in)
VALUES ('PSAA_DOB_PUBLISH_CANONICAL_TO_1CG_RULE_DOB_REQUIRED', 'PSAA_DOB_PUBLISH_CANONICAL_TO_1CG', 'RULE_DOB_REQUIRED',  TRUE),
       ('PSAA_DOB_PUBLISH_CANONICAL_TO_1CG_RULE_DOB_FORMAT_VALIDATION', 'PSAA_DOB_PUBLISH_CANONICAL_TO_1CG', 'RULE_DOB_FORMAT_VALIDATION',  TRUE);

-- Insert data into proc_step_attr_rule_msg_assoc table
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_msg_assoc (proc_step_attr_rule_assoc_id, message_id)
VALUES ('PSAA_DOB_PUBLISH_CANONICAL_TO_1CG_RULE_DOB_REQUIRED', 'MSG_FAIL_DOB_NULL'),
       ('PSAA_DOB_PUBLISH_CANONICAL_TO_1CG_RULE_DOB_FORMAT_VALIDATION', 'MSG_FAIL_DOB_INVALID');
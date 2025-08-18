/*
 * RULES:
 * 1. benefitCoverages.benefitPlanIdentifier: Must not be null.
 * 2. benefitCoverages.benefitClassIdentifier: Must not be null.
 * 3. benefitCoverages.groupPolicyNumber: Must not be null.
 */

-- Insert attributes
INSERT INTO bcpm_primary_schema.attribute (
    attribute_id,
    collection_in
)
VALUES
    ('BENEFIT_PLAN_IDENTIFIER', FALSE),
    ('BENEFIT_CLASS_IDENTIFIER', FALSE),
    ('GROUP_POLICY_NUMBER', FALSE);

-- Insert attribute list
INSERT INTO bcpm_primary_schema.attribute_list (
    attribute_list_id,
    attribute_id,
    attribute_name_tx
)
VALUES
    (0, 'BENEFIT_PLAN_IDENTIFIER', 'Benefit Plan Identifier'),
    (7, 'BENEFIT_CLASS_IDENTIFIER', 'Benefit Class Identifier'),
    (8, 'GROUP_POLICY_NUMBER', 'Group Policy Number');

-- Associate attributes with process steps
INSERT INTO bcpm_primary_schema.proc_step_attr_assoc (
    proc_step_attr_assoc_id,
    attribute_id,
    process_step_id,
    enabled_in
)
VALUES
    ('PSAA_BPI_PUBLISH_CANONICAL_TO_1CG', 'BENEFIT_PLAN_IDENTIFIER', 'PUBLISH_CANONICAL_TO_1CG', TRUE),
    ('PSAA_BCI_PUBLISH_CANONICAL_TO_1CG', 'BENEFIT_CLASS_IDENTIFIER', 'PUBLISH_CANONICAL_TO_1CG', TRUE),
    ('PSAA_GPN_PUBLISH_CANONICAL_TO_1CG', 'GROUP_POLICY_NUMBER', 'PUBLISH_CANONICAL_TO_1CG', TRUE);

-- Insert validation message
INSERT INTO bcpm_primary_schema.message (
    message_id,
    type_tx,
    body_tx,
    eff_start_date_dt,
    eff_until_date_dt,
    enabled_in
)
VALUES
    ('MSG_FAIL_BPI_BCI_GPN_NULL', 'FAIL',
    $msg$ {attributeName} must not be null or empty. $msg$,
     '2023-01-01', '9999-12-31', TRUE);

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
VALUES
(
    'RULE_BPI_BCI_GPN_REQUIRED',
    'Benefit Plan Identifier, Benefit Class Identifier and Group Policy Number must exist.',
    'NullEmptyCondition',
    '2025-01-01',
    '9999-12-31',
    TRUE,
    'MSG_FAIL_BPI_BCI_GPN_NULL',
$rule$
(
    #payload{jsonPointer} instanceof T(java.util.Collection)
        ? #payload{jsonPointer}
        : { #payload{jsonPointer} }
).![
    {
        'source': '{label}',
        'value': #this{leafPointer},
        'planType': (#this['coverageTypeCode'] != null && #this['coverageTypeCode']['code'] != null)
            ? #this['coverageTypeCode']['code']
            : ''
    }
]
.?[
    #this['value'] == null or #this['value'] == ''
]
$rule$);

-- Associate rules with attributes
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_assoc (
    proc_step_attr_rule_assoc_id,
    proc_step_attr_assoc_id,
    rule_id,
    enabled_in
)
VALUES
    ('PSAA_BPI_PUBLISH_CANONICAL_TO_1CG_RULE_BENEFIT_PLAN_IDENTIFIER_REQUIRED',
     'PSAA_BPI_PUBLISH_CANONICAL_TO_1CG', 'RULE_BPI_BCI_GPN_REQUIRED', TRUE),
    ('PSAA_BCI_PUBLISH_CANONICAL_TO_1CG_RULE_BENEFIT_CLASS_IDENTIFIER_REQUIRED',
    'PSAA_BCI_PUBLISH_CANONICAL_TO_1CG', 'RULE_BPI_BCI_GPN_REQUIRED', TRUE),
    ('PSAA_GPN_PUBLISH_CANONICAL_TO_1CG_RULE_GROUP_POLICY_NUMBER_REQUIRED',
    'PSAA_GPN_PUBLISH_CANONICAL_TO_1CG', 'RULE_BPI_BCI_GPN_REQUIRED', TRUE);

-- Associate messages with rules
INSERT INTO bcpm_primary_schema.proc_step_attr_rule_msg_assoc (
    proc_step_attr_rule_assoc_id,
    message_id
)
VALUES
    ('PSAA_BPI_PUBLISH_CANONICAL_TO_1CG_RULE_BENEFIT_PLAN_IDENTIFIER_REQUIRED', 'MSG_FAIL_BPI_BCI_GPN_NULL'),
    ('PSAA_BCI_PUBLISH_CANONICAL_TO_1CG_RULE_BENEFIT_CLASS_IDENTIFIER_REQUIRED', 'MSG_FAIL_BPI_BCI_GPN_NULL'),
    ('PSAA_GPN_PUBLISH_CANONICAL_TO_1CG_RULE_GROUP_POLICY_NUMBER_REQUIRED', 'MSG_FAIL_BPI_BCI_GPN_NULL');
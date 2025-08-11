-- Insert data into system table
INSERT INTO bcpm_primary_schema.system (system_id, name_tx, system_type_tx)
VALUES ('HOS', 'HWSE Outbound Service', 'ENROLLMENT'),
       ('CGR', 'Carrier Gateway Reporting Service', 'ENROLLMENT');

-- Insert data into process table
INSERT INTO bcpm_primary_schema.process (process_id, name_tx, system_id)
VALUES ('PUBLISH_TO_CARRIER', 'Publish HWSE Enrollments and Demographic Canonical Data', 'CGR'),
       ('HWSE_EVENT_PROCESSING', 'Process HWSE events processing data', 'HOS');

-- Insert data into tenant registry table
INSERT INTO bcpm_primary_schema.tenant_registry (tenant_id, schema_tx, database_tx, connection_tx, real_client, test_data)
VALUES ('TENANT1', 'bcpm_client_schema', 'cpmclient1_local',
        'jdbc:postgresql://localhost:5444/cpmclient1_local?currentSchema=bcpm_client_schema', TRUE, FALSE),
       ('TENANT2', 'bcpm_client_schema', 'cpmclient2_local',
        'jdbc:postgresql://localhost:5445/cpmclient2_local?currentSchema=bcpm_client_schema', TRUE, FALSE);

-- Insert data into client table
INSERT INTO bcpm_primary_schema.client (client_id, tenant_id, name_tx, oid)
VALUES (gen_random_uuid(), 'TENANT1', 'NIR2CONFIG', '98BE5B78E27003GB'),
       (gen_random_uuid(), 'TENANT2', 'NASDOMAIN', 'G3KJ1RFPEPV2XHE8');

-- Insert data into process client assoc table
INSERT INTO bcpm_primary_schema.process_client_assoc (process_id, client_id)
SELECT 'PUBLISH_TO_CARRIER', client_id FROM bcpm_primary_schema.client WHERE name_tx = 'NIR2CONFIG'
UNION ALL
SELECT 'PUBLISH_TO_CARRIER', client_id FROM bcpm_primary_schema.client WHERE name_tx = 'NASDOMAIN'
UNION ALL
SELECT 'HWSE_EVENT_PROCESSING', client_id FROM bcpm_primary_schema.client WHERE name_tx = 'NIR2CONFIG'
UNION ALL
SELECT 'HWSE_EVENT_PROCESSING', client_id FROM bcpm_primary_schema.client WHERE name_tx = 'NASDOMAIN';

-- Insert data into process_step table
INSERT INTO bcpm_primary_schema.process_step (process_step_id, name_tx, process_id, async_in, step_order)
VALUES ('CONSUME_KAFKA_FROM_GOLDEN_GATE',
        'Listen for and consume Kafka notifications sent by Golden Gate indicating changes to key HWSE events/benefits data.',
        'HWSE_EVENT_PROCESSING', FALSE, 1),
       ('BUILD_EVENT_PAYLOAD',
        'Query HWSE database as needed to build HWSE Outbound model data for each event participating in the HWSE transaction.',
        'HWSE_EVENT_PROCESSING', FALSE, 2),
       ('PUBLISH_HWSE_OUTBOUND',
        'Sends a notification to a Kafka topic with HWSE Outbound model data as the payload.',
        'HWSE_EVENT_PROCESSING', FALSE, 3),
       ('CONSUME_KAFKA_NOTIFICATION_CGR',
        'Listen for and consume Kafka notifications sent by HWSE Outbound Service indicating changes to key HWSE events/benefits data.',
        'PUBLISH_TO_CARRIER', FALSE, 1),
       ('SPLIT_PAYLOAD_BY_CARRIER',
        'Create a separate canonical data structure for each Carrier referenced in the HWSE Outbound payload.',
        'PUBLISH_TO_CARRIER', FALSE, 2),
       ('STANDARD_DERIVED_TRANSFORM',
        'Map HWSE data to complex canonical structures using derivation logic.',
        'PUBLISH_TO_CARRIER', FALSE, 3),
       ('PUBLISH_CANONICAL_TO_1CG',
        'Send individual Kafka notifications containing Canonical data payloads, with one notification per Carrier as determined by the source HWSE Outbound data.',
        'PUBLISH_TO_CARRIER', TRUE, 4);

-- Insert data into client_step_override table
INSERT INTO bcpm_primary_schema.client_step_override (client_id, override_step_id, overriden_step_id, pause_in, skip_in)
SELECT client_id, 'STANDARD_DERIVED_TRANSFORM', 'PUBLISH_CANONICAL_TO_1CG', FALSE, FALSE
FROM bcpm_primary_schema.client WHERE name_tx = 'NIR2CONFIG';

-- Insert data into remediation_policy table
INSERT INTO bcpm_primary_schema.remediation_policy (remediation_policy_id, name_tx, policy)
VALUES ('policy1', 'SSN Remediation',
        '{
            "steps": [
                {
                    "userMessage": "Notify the ADP practitioner to correct Participant SSN in the SOR.",
                    "callback": false
                }
            ]
         }'),
       ('policy2', 'Failed due to unknown Error in CGR',
        '{
            "steps": [
                {
                    "userMessage": "Notify the ADP practitioner to correct Participant SSN in the SOR.",
                    "callback": true,
                    "callbackUri": "http://localhost:8080/callback"
                }
            ]
         }');
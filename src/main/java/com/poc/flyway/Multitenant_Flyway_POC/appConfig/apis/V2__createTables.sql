-- Flyway Migration Script: Production-Grade Schema (V1__initial_schema.sql)

-- ==================== SYSTEM ====================
CREATE TABLE bcpm_primary_schema.system (
    system_id VARCHAR(255) PRIMARY KEY,
    name_tx VARCHAR(255) NOT NULL
);

-- ==================== PROCESS ====================
CREATE TABLE bcpm_primary_schema.process (
    process_id VARCHAR(255) PRIMARY KEY,
    name_tx VARCHAR(255) NOT NULL,
    system_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_process_system FOREIGN KEY (system_id) REFERENCES bcpm_primary_schema.system(system_id) ON DELETE CASCADE
);

-- ==================== TENANT REGISTRY ====================
CREATE TABLE bcpm_primary_schema.tenant_registry (
    tenant_id VARCHAR(255) PRIMARY KEY,
    schema_tx VARCHAR(255) NOT NULL,
    database_tx VARCHAR(255) NOT NULL,
    connection_tx VARCHAR(255) NOT NULL,
    real_client BOOLEAN NOT NULL DEFAULT FALSE,
    test_data BOOLEAN NOT NULL DEFAULT FALSE
);

-- ==================== CLIENT ====================
CREATE TABLE bcpm_primary_schema.client (
    client_id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    name_tx VARCHAR(255) NOT NULL,
    oid VARCHAR(255),  -- New column added per updated ER diagram
    CONSTRAINT fk_client_tenant_registry FOREIGN KEY (tenant_id) REFERENCES bcpm_primary_schema.tenant_registry(tenant_id) ON DELETE CASCADE
);


-- ==================== PROCESS CLIENT ASSOCIATION ====================
CREATE TABLE bcpm_primary_schema.process_client_assoc (
    process_id VARCHAR(255) NOT NULL,
    client_id UUID NOT NULL,
    PRIMARY KEY (process_id, client_id),
    CONSTRAINT fk_pca_process FOREIGN KEY (process_id) REFERENCES bcpm_primary_schema.process(process_id) ON DELETE CASCADE,
    CONSTRAINT fk_pca_client FOREIGN KEY (client_id) REFERENCES bcpm_primary_schema.client(client_id) ON DELETE CASCADE
);

-- ==================== PROCESS STEP ====================
CREATE TABLE bcpm_primary_schema.process_step (
    process_step_id VARCHAR(255) PRIMARY KEY,
    name_tx VARCHAR(255) NOT NULL,
    async_in BOOLEAN NOT NULL,
    process_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_process_step_process FOREIGN KEY (process_id) REFERENCES bcpm_primary_schema.process(process_id) ON DELETE CASCADE
);

-- ==================== ATTRIBUTE ====================
CREATE TABLE bcpm_primary_schema.attribute (
    attribute_id VARCHAR(255) PRIMARY KEY,
    collection_in BOOLEAN NOT NULL DEFAULT FALSE
);

-- ==================== ATTRIBUTE LIST ====================
CREATE TABLE bcpm_primary_schema.attribute_list (
    attribute_list_id SERIAL PRIMARY KEY,
    attribute_name_tx VARCHAR(255) NOT NULL,
    attribute_id VARCHAR(255) NOT NULL,
    CONSTRAINT fk_attr_list_attribute FOREIGN KEY (attribute_id) REFERENCES bcpm_primary_schema.attribute(attribute_id) ON DELETE CASCADE
);

-- ==================== PROCESS STEP ATTRIBUTE ASSOCIATION ====================
CREATE TABLE bcpm_primary_schema.proc_step_attr_assoc (
    proc_step_attr_assoc_id VARCHAR(255) PRIMARY KEY,
    attribute_id VARCHAR(255) NOT NULL,
    process_step_id VARCHAR(255) NOT NULL,
    enabled_in BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_psaa_attribute FOREIGN KEY (attribute_id) REFERENCES bcpm_primary_schema.attribute(attribute_id) ON DELETE CASCADE,
    CONSTRAINT fk_psaa_process_step FOREIGN KEY (process_step_id) REFERENCES bcpm_primary_schema.process_step(process_step_id) ON DELETE CASCADE
);

-- ==================== CLIENT STEP OVERRIDE ====================
CREATE TABLE bcpm_primary_schema.client_step_override (
    client_id UUID NOT NULL,
    override_step_id VARCHAR(255) NOT NULL,
    overriden_step_id VARCHAR(255) NOT NULL,
    pause_in BOOLEAN NOT NULL DEFAULT FALSE,
    skip_in BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (client_id, override_step_id),
    CONSTRAINT fk_cso_client FOREIGN KEY (client_id) REFERENCES bcpm_primary_schema.client(client_id) ON DELETE CASCADE,
    CONSTRAINT fk_cso_override_step FOREIGN KEY (override_step_id) REFERENCES bcpm_primary_schema.process_step(process_step_id) ON DELETE CASCADE,
    CONSTRAINT fk_cso_overriden_step FOREIGN KEY (overriden_step_id) REFERENCES bcpm_primary_schema.process_step(process_step_id) ON DELETE CASCADE
);

-- ==================== MESSAGE ====================
CREATE TABLE bcpm_primary_schema.message (
    message_id VARCHAR(255) PRIMARY KEY,
    type_tx VARCHAR(50) NOT NULL,
    body_tx TEXT NOT NULL,
    eff_start_date_dt DATE,
    eff_until_date_dt DATE,
    enabled_in BOOLEAN NOT NULL DEFAULT TRUE
);

-- ==================== RULE ====================
CREATE TABLE bcpm_primary_schema.rule (
    rule_id VARCHAR(255) PRIMARY KEY,
    name_tx VARCHAR(255) NOT NULL,
    rule_cond_id VARCHAR(255) NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    eff_start_date_dt DATE NOT NULL,
    eff_until_date_dt DATE NOT NULL,
    enabled_in BOOLEAN NOT NULL,
    rule_expression TEXT,
    CONSTRAINT fk_rule_message FOREIGN KEY (message_id) REFERENCES bcpm_primary_schema.message(message_id) ON DELETE CASCADE -- SET NULL?
);

-- ==================== PROCESS STEP ATTRIBUTE RULE ASSOCIATION ====================
CREATE TABLE bcpm_primary_schema.proc_step_attr_rule_assoc (
    proc_step_attr_rule_assoc_id VARCHAR(255) PRIMARY KEY,
    proc_step_attr_assoc_id VARCHAR(255) NOT NULL,
    rule_id VARCHAR(255) NOT NULL,
    enabled_in BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_psara_psaa FOREIGN KEY (proc_step_attr_assoc_id) REFERENCES bcpm_primary_schema.proc_step_attr_assoc(proc_step_attr_assoc_id) ON DELETE CASCADE,
    CONSTRAINT fk_psara_rule FOREIGN KEY (rule_id) REFERENCES bcpm_primary_schema.rule(rule_id) ON DELETE CASCADE
);

-- ==================== PROCESS STEP ATTRIBUTE RULE MESSAGE ASSOCIATION ====================
CREATE TABLE bcpm_primary_schema.proc_step_attr_rule_msg_assoc (
    proc_step_attr_rule_assoc_id VARCHAR(255) NOT NULL,
    message_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (proc_step_attr_rule_assoc_id, message_id),
    CONSTRAINT fk_psarma_rule_assoc FOREIGN KEY (proc_step_attr_rule_assoc_id) REFERENCES bcpm_primary_schema.proc_step_attr_rule_assoc(proc_step_attr_rule_assoc_id) ON DELETE CASCADE,
    CONSTRAINT fk_psarma_message FOREIGN KEY (message_id) REFERENCES bcpm_primary_schema.message(message_id) ON DELETE CASCADE
);

-- ==================== REMEDIATION POLICY ====================
CREATE TABLE bcpm_primary_schema.remediation_policy (
    remediation_policy_id VARCHAR(255) PRIMARY KEY,
    name_tx VARCHAR(255) NOT NULL,
    policy JSONB NOT NULL
);

-- ==================== INDEXES FOR PERFORMANCE ====================
-- CREATE INDEX idx_process_name ON process(name);
-- CREATE INDEX idx_client_name ON client(name);
-- CREATE INDEX idx_message_type ON message(type);
-- CREATE INDEX idx_rule_enabled ON rule(enabled);


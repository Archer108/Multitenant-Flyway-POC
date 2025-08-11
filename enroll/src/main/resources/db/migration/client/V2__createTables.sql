-- Create table bcpm_client_schema.process_step_outcome
CREATE TABLE bcpm_client_schema.process_step_outcome (
    process_step_outcome_id VARCHAR(255) PRIMARY KEY,
    correlation_id          VARCHAR(255) NOT NULL,
    process_step_id         VARCHAR(255) NOT NULL,
    status_tx               VARCHAR(255) NOT NULL,
    next_step_id            VARCHAR(255), -- Nullable. Required only if status is "STOP"
    client_id               VARCHAR(255) NOT NULL,
    transmission_id         VARCHAR(255) NOT NULL,
    state_tx                VARCHAR(255),
    override_process_step_outcome_id    VARCHAR(255),
    startingTimelineDate_dt  TIMESTAMP WITH TIME ZONE,
    aoid                     VARCHAR(255),
    resume_in               BOOLEAN DEFAULT FALSE,
    created_on_dt           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_on_dt          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    author                  VARCHAR(255),
    version_num             INTEGER DEFAULT 1
);

-- Create Table bcpm_client_schema.remediation_support
CREATE TABLE bcpm_client_schema.remediation_support (
    process_step_outcome_id VARCHAR(255) PRIMARY KEY,
    remediation_policy_id   VARCHAR(255) NOT NULL,
    support_plan_tx         VARCHAR(255) NOT NULL, -- TODO: Should this be a JSON?
    CONSTRAINT fk_remediation_support_process_step_outcome FOREIGN KEY (process_step_outcome_id) REFERENCES bcpm_client_schema.process_step_outcome (process_step_outcome_id) ON DELETE CASCADE
);

-- Create Table bcpm_client_schema.messages
CREATE TABLE bcpm_client_schema.message (
    message_id              VARCHAR(255) PRIMARY KEY,
    process_step_outcome_id VARCHAR(255) NOT NULL,
    message                 JSONB NOT NULL,
    created_on_dt           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_on_dt          TIMESTAMP WITH TIME ZONE,
    author                  VARCHAR(255),
    version_num             INTEGER DEFAULT 1,
    CONSTRAINT fk_message_process_step_outcome FOREIGN KEY (process_step_outcome_id) REFERENCES bcpm_client_schema.process_step_outcome (process_step_outcome_id) ON DELETE CASCADE
);

-- Create Table action_context
CREATE TABLE bcpm_client_schema.action_context (
    process_step_outcome_id VARCHAR(255) NOT NULL,
    transmission_id         VARCHAR(255) PRIMARY KEY,
    payload                 JSONB,
    created_on_dt           TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    modified_on_dt          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    author                  VARCHAR(255),
    version_num             INTEGER DEFAULT 1,
    CONSTRAINT fk_action_context_process_step_outcome FOREIGN KEY (process_step_outcome_id) REFERENCES bcpm_client_schema.process_step_outcome (process_step_outcome_id) ON DELETE CASCADE
);

-- Create Table bcpm_client_schema.enrollment_info
CREATE TABLE bcpm_client_schema.enrollment_info (
    enroll_info_id      VARCHAR(255) PRIMARY KEY,
    process_step_outcome_id VARCHAR(255)  NOT NULL UNIQUE,
    carrier_id              VARCHAR(255) NOT NULL,
    CONSTRAINT fk_enrollment_info_process_step_outcome FOREIGN KEY (process_step_outcome_id) REFERENCES bcpm_client_schema.process_step_outcome (process_step_outcome_id) ON DELETE CASCADE
);

CREATE TABLE bcpm_client_schema.plan_type (
    plan_type_id      VARCHAR(255) PRIMARY KEY,
    plan_name          VARCHAR(255)  NOT NULL
);

CREATE TABLE bcpm_client_schema.plan_type_enroll_info_assoc (
    plan_type_id      VARCHAR(255)  NOT NULL,
    enroll_info_id VARCHAR(255)  NOT NULL,
    PRIMARY KEY (plan_type_id, enroll_info_id),
    CONSTRAINT fk_plan_type_enroll_info_assoc_plan_type FOREIGN KEY (plan_type_id) REFERENCES bcpm_client_schema.plan_type (plan_type_id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_type_enroll_info_assoc_enroll_info FOREIGN KEY (enroll_info_id) REFERENCES bcpm_client_schema.enrollment_info (enroll_info_id) ON DELETE CASCADE
);


CREATE TABLE bcpm_client_schema.participant
(
    participant_id      VARCHAR(255)  PRIMARY KEY,
    client_id           VARCHAR(255)  NOT NULL,
    first_name          VARCHAR(255),
    last_name           VARCHAR(255),
    ssn                 VARCHAR(255),
    job_id              VARCHAR(255),
    participant_type_tx VARCHAR(255),
    job_title           VARCHAR(255),
    aoid                VARCHAR(255)
);
-- Temporary Test Table
CREATE TABLE bcpm_client_schema.client_test_db (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at DATE DEFAULT CURRENT_DATE,
    description TEXT,
    payload    JSONB
);
-- ===========================
-- TENANT DB (client) INDEXES
-- ===========================

-- Enable trigram (once per DATABASE)
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 1) process_step_outcome: primary ordering & filters
CREATE INDEX IF NOT EXISTS idx_pso_created_on_dt__pso_id
  ON bcpm_client_schema.process_step_outcome (created_on_dt, process_step_outcome_id);

CREATE INDEX IF NOT EXISTS idx_pso_status_tx
  ON bcpm_client_schema.process_step_outcome (status_tx);

CREATE INDEX IF NOT EXISTS idx_pso_state_tx
  ON bcpm_client_schema.process_step_outcome (state_tx);

CREATE INDEX IF NOT EXISTS idx_pso_process_step_id
  ON bcpm_client_schema.process_step_outcome (process_step_id);

CREATE INDEX IF NOT EXISTS idx_pso_client_id
  ON bcpm_client_schema.process_step_outcome (client_id);

CREATE INDEX IF NOT EXISTS idx_pso_aoid
  ON bcpm_client_schema.process_step_outcome (aoid);

-- Optional: speed up ILIKE on these fields used by the global search
CREATE INDEX IF NOT EXISTS gin_pso_status_tx_trgm
  ON bcpm_client_schema.process_step_outcome USING gin (lower(status_tx) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS gin_pso_state_tx_trgm
  ON bcpm_client_schema.process_step_outcome USING gin (lower(state_tx) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS gin_pso_aoid_trgm
  ON bcpm_client_schema.process_step_outcome USING gin (lower(aoid) gin_trgm_ops);

-- 2) enrollment_info join + carrier filter
CREATE INDEX IF NOT EXISTS idx_ei_pso_id
  ON bcpm_client_schema.enrollment_info (process_step_outcome_id);

CREATE INDEX IF NOT EXISTS idx_ei_carrier_id
  ON bcpm_client_schema.enrollment_info (carrier_id);

-- Optional: carrier ILIKE in global search
CREATE INDEX IF NOT EXISTS gin_ei_carrier_id_trgm
  ON bcpm_client_schema.enrollment_info USING gin (lower(carrier_id) gin_trgm_ops);

-- 3) participant join + name search (used by global search: first/last/full name)
CREATE INDEX IF NOT EXISTS idx_participant_aoid
  ON bcpm_client_schema.participant (aoid);

CREATE INDEX IF NOT EXISTS gin_participant_first_trgm
  ON bcpm_client_schema.participant USING gin (lower(first_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS gin_participant_last_trgm
  ON bcpm_client_schema.participant USING gin (lower(last_name) gin_trgm_ops);

-- Optional: speed up "first last" and "last, first" searches (expression indexes)
CREATE INDEX IF NOT EXISTS gin_participant_fullname_trgm
  ON bcpm_client_schema.participant USING gin (lower(first_name || ' ' || last_name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS gin_participant_revname_trgm
  ON bcpm_client_schema.participant USING gin (lower(last_name || ', ' || first_name) gin_trgm_ops);
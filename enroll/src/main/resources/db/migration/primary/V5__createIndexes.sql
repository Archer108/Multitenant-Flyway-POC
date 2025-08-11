-- ===========================
-- PRIMARY DB INDEXES
-- ===========================

-- Enable trigram once per DATABASE
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- client(name_tx) is searched by substring to translate search -> OIDs
CREATE INDEX IF NOT EXISTS gin_client_name_tx_trgm
  ON bcpm_primary_schema.client USING gin (lower(name_tx) gin_trgm_ops);

-- Frequently used exact lookups
CREATE INDEX IF NOT EXISTS idx_client_oid
  ON bcpm_primary_schema.client (oid);

-- System type filter path = system.system_type_tx -> process -> process_step
CREATE INDEX IF NOT EXISTS idx_system_system_type_tx
  ON bcpm_primary_schema.system (system_type_tx);

CREATE INDEX IF NOT EXISTS idx_process_system_id
  ON bcpm_primary_schema.process (system_id);

CREATE INDEX IF NOT EXISTS idx_process_step_process_id
  ON bcpm_primary_schema.process_step (process_id);
CREATE TABLE IF NOT EXISTS bcpm_client_schema.client_test_db (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    created_at DATE NOT NULL,
    description TEXT,
    payload JSONB
);
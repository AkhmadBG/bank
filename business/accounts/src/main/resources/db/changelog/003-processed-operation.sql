CREATE TABLE IF NOT EXISTS processed_operation (
    operation_id UUID PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    login VARCHAR(100) NOT NULL,
    counterparty VARCHAR(100) NOT NULL DEFAULT '',
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_processed_operation_created_at ON processed_operation (created_at);
CREATE TABLE IF NOT EXISTS transfer_operations (
    transfer_operation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login_recipient VARCHAR(150) NOT NULL,
    login_sender VARCHAR(150) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    operation_date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_transfer_operations_login_sender ON transfer_operations (login_sender);
CREATE INDEX IF NOT EXISTS idx_transfer_operations_login_recipient ON transfer_operations (login_recipient);
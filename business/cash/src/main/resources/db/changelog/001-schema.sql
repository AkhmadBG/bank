CREATE TABLE IF NOT EXISTS cash_operations (
    cash_operation_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    login VARCHAR(100) NOT NULL,
    cash_operation_type VARCHAR(50) NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    operation_date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_cash_operations_login ON cash_operations (login);
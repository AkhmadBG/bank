ALTER TABLE cash_operations
    ADD COLUMN operation_id UUID;

CREATE UNIQUE INDEX uq_cash_operations_operation_id
    ON cash_operations (operation_id);
ALTER TABLE transfer_operations
    ADD COLUMN operation_id UUID;

CREATE UNIQUE INDEX uq_transfer_operations_operation_id
    ON transfer_operations (operation_id);
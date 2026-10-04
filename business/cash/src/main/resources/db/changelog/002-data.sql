INSERT INTO cash_operations (login, cash_operation_type, amount, operation_date_time)
VALUES ('ivanov', 'PUT', 3000.00, '2026-08-01 10:15:00')
    ON CONFLICT DO NOTHING;

INSERT INTO cash_operations (login, cash_operation_type, amount, operation_date_time)
VALUES ('ivanov', 'GET', 500.00, '2026-08-15 14:30:00')
    ON CONFLICT DO NOTHING;

INSERT INTO cash_operations (login, cash_operation_type, amount, operation_date_time)
VALUES ('petrov', 'PUT', 7000.00, '2026-08-03 09:00:00')
    ON CONFLICT DO NOTHING;

INSERT INTO cash_operations (login, cash_operation_type, amount, operation_date_time)
VALUES ('sidorova', 'GET', 1200.00, '2026-08-20 17:45:00')
    ON CONFLICT DO NOTHING;
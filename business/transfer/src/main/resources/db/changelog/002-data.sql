INSERT INTO transfer_operations (login_sender, login_recipient, amount, operation_date_time)
VALUES ('ivanov', 'petrov', 1000.00, '2026-08-05 12:00:00')
    ON CONFLICT DO NOTHING;

INSERT INTO transfer_operations (login_sender, login_recipient, amount, operation_date_time)
VALUES ('petrov', 'sidorova', 2500.00, '2026-08-10 16:20:00')
    ON CONFLICT DO NOTHING;

INSERT INTO transfer_operations (login_sender, login_recipient, amount, operation_date_time)
VALUES ('kozlov', 'ivanov', 500.00, '2026-08-18 11:05:00')
    ON CONFLICT DO NOTHING;
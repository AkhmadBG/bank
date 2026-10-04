INSERT INTO notifications (recipient, description, created_at)
VALUES ('ivanov', 'Пополнение счёта на 3000.00', '2026-08-01 10:15:05')
    ON CONFLICT DO NOTHING;

INSERT INTO notifications (recipient, description, created_at)
VALUES ('ivanov', 'Перевод получателю petrov на сумму 1000.00', '2026-08-05 12:00:05')
    ON CONFLICT DO NOTHING;

INSERT INTO notifications (recipient, description, created_at)
VALUES ('petrov', 'Получен перевод от ivanov на сумму 1000.00', '2026-08-05 12:00:05')
    ON CONFLICT DO NOTHING;

INSERT INTO notifications (recipient, description, created_at)
VALUES ('sidorova', 'Получен перевод от petrov на сумму 2500.00', '2026-08-10 16:20:05')
    ON CONFLICT DO NOTHING;
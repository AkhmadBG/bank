INSERT INTO accounts (login, name, birthdate, balance)
VALUES ('ivanov', 'Иванов Иван', '1990-05-14', 5000.00)
    ON CONFLICT (login) DO NOTHING;

INSERT INTO accounts (login, name, birthdate, balance)
VALUES ('petrov', 'Петров Пётр', '1985-11-02', 10000.00)
    ON CONFLICT (login) DO NOTHING;

INSERT INTO accounts (login, name, birthdate, balance)
VALUES ('sidorova', 'Сидорова Анна', '1995-03-21', 15000.00)
    ON CONFLICT (login) DO NOTHING;

INSERT INTO accounts (login, name, birthdate, balance)
VALUES ('kozlov', 'Козлов Олег', '2000-07-09', 20000.00)
    ON CONFLICT (login) DO NOTHING;
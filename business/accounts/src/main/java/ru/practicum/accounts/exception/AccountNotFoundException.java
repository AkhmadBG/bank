package ru.practicum.accounts.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String login) {
        super("Аккаунт не найден: " + login);
    }

}
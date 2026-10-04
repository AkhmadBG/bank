package ru.practicum.accounts.exception;

public class InvalidAgeException extends RuntimeException {

    public InvalidAgeException() {
        super("Возраст должен быть не менее 18 лет");
    }

}
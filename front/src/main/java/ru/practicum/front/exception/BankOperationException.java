package ru.practicum.front.exception;

public class BankOperationException extends RuntimeException {
    public BankOperationException(String message) {
        super(message);
    }
}
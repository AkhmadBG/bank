package ru.practicum.accounts.exception;

import java.util.UUID;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(UUID operationId) {
        super("Ключ операции " + operationId + " уже использован с другими параметрами");
    }

}
package ru.practicum.transfer.exception;

import org.springframework.http.HttpStatusCode;

public class AccountCallException extends RuntimeException {

    private final HttpStatusCode status;

    public AccountCallException(String message, HttpStatusCode status) {
        super(message);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

}
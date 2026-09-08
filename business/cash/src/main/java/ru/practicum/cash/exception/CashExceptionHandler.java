package ru.practicum.cash.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import ru.practicum.interaction.exception.ApiError;

@RestControllerAdvice
public class CashExceptionHandler {

    @ExceptionHandler(AccountCallException.class)
    public ResponseEntity<ApiError> handleAccountCall(AccountCallException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Некорректные данные запроса");
        return ResponseEntity.badRequest().body(new ApiError(message));
    }

}
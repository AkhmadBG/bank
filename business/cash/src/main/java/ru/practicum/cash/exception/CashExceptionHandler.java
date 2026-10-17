package ru.practicum.cash.exception;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.practicum.interaction.exception.AccountCallException;
import ru.practicum.interaction.exception.ApiError;

@Slf4j
@RestControllerAdvice
public class CashExceptionHandler {

    private static final String ACCOUNTS_UNAVAILABLE = "Сервис аккаунтов временно недоступен";

    @ExceptionHandler(AccountCallException.class)
    public ResponseEntity<ApiError> handleAccountCall(AccountCallException e) {
        return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getMessage()));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiError> handleCircuitOpen(CallNotPermittedException e) {
        log.warn("Выключатель '{}' открыт, запрос в Accounts не выполнен", e.getCausingCircuitBreakerName());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ACCOUNTS_UNAVAILABLE));
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiError> handleFeign(FeignException e) {
        log.error("Ошибка вызова Accounts: status={}, message={}", e.status(), e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(ACCOUNTS_UNAVAILABLE));
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
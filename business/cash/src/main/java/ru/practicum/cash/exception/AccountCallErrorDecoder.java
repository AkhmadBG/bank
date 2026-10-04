package ru.practicum.cash.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import ru.practicum.interaction.exception.ApiError;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class AccountCallErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public AccountCallErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400 && response.status() < 500 && response.body() != null) {
            try (InputStream body = response.body().asInputStream()) {
                ApiError apiError = objectMapper.readValue(body, ApiError.class);
                return new AccountCallException(apiError.message(), org.springframework.http.HttpStatusCode.valueOf(response.status()));
            } catch (IOException e) {
                return new AccountCallException("Сервис аккаунтов вернул ошибку", org.springframework.http.HttpStatusCode.valueOf(response.status()));
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }

}
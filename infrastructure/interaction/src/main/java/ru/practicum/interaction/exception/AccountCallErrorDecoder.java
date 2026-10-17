package ru.practicum.interaction.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.http.HttpStatusCode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class AccountCallErrorDecoder implements ErrorDecoder {

    private static final String DEFAULT_MESSAGE = "Сервис аккаунтов вернул ошибку";

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public AccountCallErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        if (status >= 400 && status < 500) {
            return new AccountCallException(readMessage(response), HttpStatusCode.valueOf(status));
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private String readMessage(Response response) {
        if (response.body() == null) {
            return DEFAULT_MESSAGE;
        }
        try (InputStream body = response.body().asInputStream()) {
            ApiError apiError = objectMapper.readValue(body, ApiError.class);
            return apiError != null && apiError.message() != null ? apiError.message() : DEFAULT_MESSAGE;
        } catch (IOException | RuntimeException e) {
            return DEFAULT_MESSAGE;
        }
    }

}
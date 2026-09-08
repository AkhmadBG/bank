package ru.practicum.front.exception;

import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import ru.practicum.interaction.exception.ApiError;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class GatewayErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new Default();

    public GatewayErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400 && response.status() < 500 && response.body() != null) {
            try (InputStream body = response.body().asInputStream()) {
                ApiError apiError = objectMapper.readValue(body, ApiError.class);
                return new BankOperationException(apiError.message());
            } catch (IOException e) {
                return new BankOperationException("Не удалось обработать ошибку сервиса");
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }

}
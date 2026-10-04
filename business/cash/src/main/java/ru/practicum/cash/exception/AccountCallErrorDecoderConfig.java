package ru.practicum.cash.exception;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

public class AccountCallErrorDecoderConfig {

    @Bean
    public ErrorDecoder errorDecoder(ObjectMapper objectMapper) {
        return new AccountCallErrorDecoder(objectMapper);
    }

}
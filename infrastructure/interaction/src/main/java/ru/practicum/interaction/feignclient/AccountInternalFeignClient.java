package ru.practicum.interaction.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.AccountInternalOperations;
import ru.practicum.interaction.exception.AccountCallErrorDecoderConfig;

@FeignClient(
        name = "accounts",
        url = "${services.accounts.url:http://accounts:8082}",
        contextId = "transferAccountInternalClient",
        configuration = AccountCallErrorDecoderConfig.class
)
public interface AccountInternalFeignClient extends AccountInternalOperations {
}
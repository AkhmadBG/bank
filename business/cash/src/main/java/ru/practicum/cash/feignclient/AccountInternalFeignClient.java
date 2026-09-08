package ru.practicum.cash.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.cash.exception.AccountCallErrorDecoderConfig;
import ru.practicum.interaction.apiinterface.AccountInternalOperations;

@FeignClient(name = "accounts", contextId = "accountInternalClient", configuration = AccountCallErrorDecoderConfig.class)
public interface AccountInternalFeignClient extends AccountInternalOperations {
}
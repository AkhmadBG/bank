package ru.practicum.transfer.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.AccountInternalOperations;
import ru.practicum.transfer.exception.AccountCallErrorDecoderConfig;

@FeignClient(name = "accounts", contextId = "transferAccountInternalClient", configuration = AccountCallErrorDecoderConfig.class)
public interface AccountInternalFeignClient extends AccountInternalOperations {
}
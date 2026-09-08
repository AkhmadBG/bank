package ru.practicum.front.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.front.config.GatewayFeignClientConfig;
import ru.practicum.interaction.apiinterface.AccountOperations;

@FeignClient(name = "gateway", path = "/account",
        contextId = "gatewayAccountClient", configuration = GatewayFeignClientConfig.class)
public interface GatewayAccountFeignClient extends AccountOperations {
}
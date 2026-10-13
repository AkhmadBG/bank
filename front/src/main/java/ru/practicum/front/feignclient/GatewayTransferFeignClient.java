package ru.practicum.front.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.front.config.GatewayFeignClientConfig;
import ru.practicum.interaction.apiinterface.TransferOperations;

@FeignClient(
        name = "gateway",
        path = "/transfer",
        contextId = "gatewayTransferClient",
        configuration = GatewayFeignClientConfig.class,
        url = "${services.gateway.url:http://gateway:8081}"
)
public interface GatewayTransferFeignClient extends TransferOperations {
}
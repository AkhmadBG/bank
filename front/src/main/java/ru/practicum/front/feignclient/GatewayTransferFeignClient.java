package ru.practicum.front.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.front.config.GatewayFeignClientConfig;
import ru.practicum.interaction.apiinterface.TransferOperations;

@FeignClient(name = "gateway", path = "/transfer",
        contextId = "gatewayTransferClient", configuration = GatewayFeignClientConfig.class)
public interface GatewayTransferFeignClient extends TransferOperations {
}
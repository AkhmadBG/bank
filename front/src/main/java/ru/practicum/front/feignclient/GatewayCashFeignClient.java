package ru.practicum.front.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.front.config.GatewayFeignClientConfig;
import ru.practicum.interaction.apiinterface.CashOperations;

@FeignClient(name = "gateway", path = "/cash",
        contextId = "gatewayCashClient", configuration = GatewayFeignClientConfig.class)
public interface GatewayCashFeignClient extends CashOperations {
}
package ru.practicum.interaction.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.AccountOperations;

@FeignClient(name = "account", path = "/account")
public interface AccountFeignClient extends AccountOperations {
}

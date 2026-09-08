package ru.practicum.interaction.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.CashOperations;

@FeignClient(name = "cash", path = "/cash")
public interface CashFeignClient extends CashOperations {
}

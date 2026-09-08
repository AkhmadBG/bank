package ru.practicum.interaction.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.TransferOperations;

@FeignClient(name = "transfer", path = "/transfer")
public interface TransferFeignClient extends TransferOperations {
}

package ru.practicum.interaction.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.NotificationOperations;

@FeignClient(name = "notifications", path = "/notification")
public interface NotificationFeignClient  extends NotificationOperations {
}
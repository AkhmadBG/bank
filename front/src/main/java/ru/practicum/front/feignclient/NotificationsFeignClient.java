package ru.practicum.front.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.interaction.front.dto.NotificationDto;

import java.util.List;

@FeignClient(
        name = "notifications",
        url = "${services.notifications.url:http://notifications:8085}"
)
public interface NotificationsFeignClient {

    @GetMapping("/api/notifications/{recipient}")
    List<NotificationDto> getNotifications(
            @PathVariable("recipient") String recipient
    );

}
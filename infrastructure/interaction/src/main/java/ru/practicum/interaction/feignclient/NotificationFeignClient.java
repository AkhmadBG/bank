package ru.practicum.interaction.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.interaction.apiinterface.NotificationOperations;

//@FeignClient(
//        name = "notifications",
//        url = "${services.notifications.url:http://notifications:8085}",
//        path = "/notification"
//)
//public interface NotificationFeignClient extends NotificationOperations {
//}
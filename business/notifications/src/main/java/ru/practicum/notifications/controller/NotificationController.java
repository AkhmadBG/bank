package ru.practicum.notifications.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.notifications.entity.Notification;
import ru.practicum.notifications.service.NotificationService;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/{recipient}")
    public List<Notification> getNotifications(
            @PathVariable String recipient
    ) {
        return notificationService.findByRecipient(recipient);
    }

}
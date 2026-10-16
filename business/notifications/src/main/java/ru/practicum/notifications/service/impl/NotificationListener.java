package ru.practicum.notifications.service.impl;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.kafka.KafkaTopics;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.notifications.service.NotificationService;

@Component
public class NotificationListener {

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATIONS)
    public void onMessage(NotificationMessage message) {
        notificationService.send(message);
    }

}
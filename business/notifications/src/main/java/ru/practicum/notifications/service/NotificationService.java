package ru.practicum.notifications.service;

//import ru.practicum.interaction.notification.dto.NotificationRequest;

import ru.practicum.interaction.kafka.NotificationMessage;

public interface NotificationService {

//    void sendNotification(NotificationRequest notificationRequest);
    void send(NotificationMessage message);

}
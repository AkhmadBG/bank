package ru.practicum.notifications.service;

import ru.practicum.interaction.notification.dto.NotificationRequest;

public interface NotificationService {

    void sendNotification(NotificationRequest notificationRequest);

}
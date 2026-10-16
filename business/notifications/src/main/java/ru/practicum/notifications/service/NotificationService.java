package ru.practicum.notifications.service;

import ru.practicum.interaction.kafka.NotificationMessage;

public interface NotificationService {

    void send(NotificationMessage message);

}
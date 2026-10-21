package ru.practicum.notifications.service;

import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.notifications.entity.Notification;

import java.util.List;

public interface NotificationService {

    void send(NotificationMessage message);

    List<Notification> findByRecipient(String recipient);

}
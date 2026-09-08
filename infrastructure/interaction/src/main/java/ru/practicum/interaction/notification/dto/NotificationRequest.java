package ru.practicum.interaction.notification.dto;

public record NotificationRequest(

        String recipient,

        String description

) {
}

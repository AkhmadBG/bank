package ru.practicum.interaction.front.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDto(

        Long notificationId,

        UUID eventId,

        String recipient,

        String description,

        LocalDateTime createdAt

) {
}
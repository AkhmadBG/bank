package ru.practicum.interaction.kafka;

import java.math.BigDecimal;
import java.util.UUID;

public record NotificationMessage(

        UUID eventId,

        String login,

        NotificationType type,

        String message,
        
        BigDecimal amount

) {
}
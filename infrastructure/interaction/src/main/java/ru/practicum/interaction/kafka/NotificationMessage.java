package ru.practicum.interaction.kafka;

import java.math.BigDecimal;

public record NotificationMessage(

        String login,

        NotificationType type,

        String message,
        
        BigDecimal amount

) {
}
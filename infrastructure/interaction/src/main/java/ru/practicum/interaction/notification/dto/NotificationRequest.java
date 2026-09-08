package ru.practicum.interaction.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(

        @NotBlank
        String recipient,

        @NotBlank
        String description

) {}
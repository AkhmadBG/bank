package ru.practicum.interaction.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record InternalTransferRequest(

        UUID operationId,

        String loginSender,

        String loginRecipient,

        @NotNull(message = "Сумма обязательна")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Сумма должна быть больше нуля"
        )
        BigDecimal amount

) {
}
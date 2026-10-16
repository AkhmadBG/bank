package ru.practicum.interaction.transfer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferOperationRequest(

        @NotBlank
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
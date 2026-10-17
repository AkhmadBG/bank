package ru.practicum.interaction.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceChangeRequest(

        UUID operationId,

        String login,

        CashAction cashAction,

        @NotNull(message = "Сумма обязательна")
        @DecimalMin(
                value = "0.0",
                inclusive = false,
                message = "Сумма должна быть больше нуля"
        )
        BigDecimal amount

) {
}
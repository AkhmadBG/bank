package ru.practicum.interaction.cash.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;

public record CashOperationRequest(

        @NotNull
        CashAction cashAction,

        @NotNull
        @Positive
        BigDecimal amount

) {
}
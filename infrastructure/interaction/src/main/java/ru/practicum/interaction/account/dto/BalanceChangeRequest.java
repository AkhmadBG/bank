package ru.practicum.interaction.account.dto;

import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;

public record BalanceChangeRequest(

        String login,

        CashAction cashAction,

        BigDecimal amount

) {}
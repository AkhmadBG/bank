package ru.practicum.interaction.account.dto;

import java.math.BigDecimal;

public record InternalTransferRequest(

        String loginSender,

        String loginRecipient,

        BigDecimal amount

) {
}
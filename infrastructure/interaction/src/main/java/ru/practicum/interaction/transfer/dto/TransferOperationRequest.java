package ru.practicum.interaction.transfer.dto;

import java.math.BigDecimal;

public record TransferOperationRequest(

        String loginRecipient,

        BigDecimal amount

) {
}
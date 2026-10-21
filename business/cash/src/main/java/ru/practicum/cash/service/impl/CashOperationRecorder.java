
package ru.practicum.cash.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.cash.repository.CashOperationRepository;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CashOperationRecorder {

    private final CashOperationRepository cashOperationRepository;

    @Transactional
    public boolean record(
            UUID operationId,
            String login,
            CashAction action,
            BigDecimal amount
    ) {
        int inserted = cashOperationRepository.insertIfAbsent(
                operationId,
                login,
                action.name(),
                amount
        );

        return inserted == 1;
    }

}
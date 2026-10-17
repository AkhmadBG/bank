package ru.practicum.cash.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.cash.entity.CashOperation;
import ru.practicum.cash.repository.CashOperationRepository;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CashOperationRecorder {

    private final CashOperationRepository cashOperationRepository;

    @Transactional
    public void record(String login, CashAction action, BigDecimal amount) {
        CashOperation operation = CashOperation.builder()
                .login(login)
                .cashOperationType(action)
                .amount(amount)
                .operationDateTime(LocalDateTime.now())
                .build();

        cashOperationRepository.save(operation);
    }

}
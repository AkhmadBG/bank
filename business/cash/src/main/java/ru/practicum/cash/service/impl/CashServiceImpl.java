package ru.practicum.cash.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.config.CurrentUserProvider;
import ru.practicum.cash.service.CashService;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.util.ApplicationUtil;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final AccountsClient accountsClient;
    private final CashOperationRecorder operationRecorder;
    private final CurrentUserProvider currentUserProvider;
    private final MeterRegistry meterRegistry;

    @Override
    public void cashOperation(UUID operationId, CashOperationRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        String login = currentUserProvider.getCurrentLogin();

        try {
            accountsClient.changeBalance(
                    operationId,
                    login,
                    request.cashAction(),
                    request.amount()
            );
        } catch (RuntimeException e) {
            if (request.cashAction() == CashAction.GET) {
                meterRegistry.counter(
                        "cash.withdrawal.failures",
                        "login", login
                ).increment();
            }

            log.warn("Cash operation failed for login={}", login);
            throw e;
        }

        operationRecorder.record(
                operationId,
                login,
                request.cashAction(),
                request.amount()
        );
    }

}
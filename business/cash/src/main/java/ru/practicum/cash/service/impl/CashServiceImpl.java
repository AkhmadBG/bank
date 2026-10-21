package ru.practicum.cash.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.config.CurrentUserProvider;
import ru.practicum.cash.service.CashService;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.util.ApplicationUtil;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final AccountsClient accountsClient;
    private final CashOperationRecorder operationRecorder;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void cashOperation(UUID operationId, CashOperationRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        String login = currentUserProvider.getCurrentLogin();

        accountsClient.changeBalance(operationId, login, request.cashAction(), request.amount());

        operationRecorder.record(operationId, login, request.cashAction(), request.amount());
    }

}
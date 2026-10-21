package ru.practicum.transfer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.interaction.util.ApplicationUtil;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.config.CurrentUserProvider;
import ru.practicum.transfer.exception.SelfTransferException;
import ru.practicum.transfer.service.TransferService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountsClient accountsClient;
    private final CurrentUserProvider currentUserProvider;
    private final TransferOperationRecorder transferOperationRecorder;

    @Override
    public void transfer(UUID operationId, TransferOperationRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        String senderLogin = currentUserProvider.getCurrentLogin();

        if (senderLogin.equals(request.loginRecipient())) {
            throw new SelfTransferException();
        }

        accountsClient.transferBalance(operationId, senderLogin, request.loginRecipient(), request.amount());

        transferOperationRecorder.record(operationId, senderLogin, request.loginRecipient(), request.amount());
    }

}
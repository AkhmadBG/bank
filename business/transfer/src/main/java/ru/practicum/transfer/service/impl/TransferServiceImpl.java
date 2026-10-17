package ru.practicum.transfer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationPublisher;
import ru.practicum.interaction.kafka.NotificationType;
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
    private final TransferOperationRecorder operationRecorder;
    private final NotificationPublisher notificationPublisher;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void transfer(TransferOperationRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        String senderLogin = currentUserProvider.getCurrentLogin();

        if (senderLogin.equals(request.loginRecipient())) {
            throw new SelfTransferException();
        }

        UUID operationId = UUID.randomUUID();

        accountsClient.transferBalance(operationId, senderLogin, request.loginRecipient(), request.amount());
        operationRecorder.record(senderLogin, request.loginRecipient(), request.amount());

        notifySender(senderLogin, request);
        notifyRecipient(senderLogin, request);
    }

    private void notifySender(String senderLogin, TransferOperationRequest request) {
        notificationPublisher.publish(new NotificationMessage(
                UUID.randomUUID(), senderLogin, NotificationType.TRANSFER,
                "Перевод " + request.amount() + " пользователю " + request.loginRecipient(),
                request.amount()));
    }

    private void notifyRecipient(String senderLogin, TransferOperationRequest request) {
        notificationPublisher.publish(new NotificationMessage(
                UUID.randomUUID(), request.loginRecipient(), NotificationType.TRANSFER,
                "Поступил перевод " + request.amount() + " от " + senderLogin,
                request.amount()));
    }

}
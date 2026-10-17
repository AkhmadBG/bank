package ru.practicum.cash.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.config.CurrentUserProvider;
import ru.practicum.cash.service.CashService;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationPublisher;
import ru.practicum.interaction.kafka.NotificationType;
import ru.practicum.interaction.util.ApplicationUtil;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final AccountsClient accountsClient;
    private final NotificationPublisher notificationPublisher;
    private final CashOperationRecorder operationRecorder;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void cashOperation(CashOperationRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        String login = currentUserProvider.getCurrentLogin();

        UUID operationId = UUID.randomUUID();
        accountsClient.changeBalance(operationId, login, request.cashAction(), request.amount());
        operationRecorder.record(login, request.cashAction(), request.amount());
        sendNotification(login, request);
    }

    private void sendNotification(String login, CashOperationRequest request) {
        NotificationType type = request.cashAction() == CashAction.PUT
                ? NotificationType.DEPOSIT : NotificationType.WITHDRAW;
        String text = (type == NotificationType.DEPOSIT ? "Пополнение счёта на " : "Снятие со счёта ")
                + request.amount();

        notificationPublisher.publish(
                new NotificationMessage(UUID.randomUUID(), login, type, text, request.amount()));
    }

}
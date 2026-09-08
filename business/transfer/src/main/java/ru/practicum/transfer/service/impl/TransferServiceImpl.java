package ru.practicum.transfer.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.feignclient.NotificationFeignClient;
import ru.practicum.interaction.notification.dto.NotificationRequest;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.transfer.entity.TransferOperation;
import ru.practicum.transfer.exception.SelfTransferException;
import ru.practicum.transfer.feignclient.AccountInternalFeignClient;
import ru.practicum.transfer.repository.TransferOperationRepository;
import ru.practicum.transfer.config.CurrentUserProvider;
import ru.practicum.transfer.service.TransferService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountInternalFeignClient accountInternalFeignClient;
    private final NotificationFeignClient notificationFeignClient;
    private final TransferOperationRepository transferOperationRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void transfer(TransferOperationRequest request) {
        String senderLogin = currentUserProvider.getCurrentLogin();

        if (senderLogin.equals(request.loginRecipient())) {
            throw new SelfTransferException();
        }

        accountInternalFeignClient.transferBalance(
                new InternalTransferRequest(senderLogin, request.loginRecipient(), request.amount())
        );

        persistOperation(senderLogin, request);
        sendNotificationBestEffort(senderLogin, request);
    }

    @Transactional
    protected void persistOperation(String senderLogin, TransferOperationRequest request) {
        TransferOperation operation = TransferOperation.builder()
                .loginSender(senderLogin)
                .loginRecipient(request.loginRecipient())
                .amount(request.amount())
                .operationDateTime(LocalDateTime.now())
                .build();

        transferOperationRepository.save(operation);
    }

    private void sendNotificationBestEffort(String senderLogin, TransferOperationRequest request) {
        try {
            notificationFeignClient.sendNotification(new NotificationRequest(
                    senderLogin, "Перевод получателю " + request.loginRecipient() + " на сумму " + request.amount()));
        } catch (Exception e) {
            log.warn("Не удалось отправить уведомление отправителю {}: {}", senderLogin, e.getMessage());
        }

        try {
            notificationFeignClient.sendNotification(new NotificationRequest(
                    request.loginRecipient(), "Получен перевод от " + senderLogin + " на сумму " + request.amount()));
        } catch (Exception e) {
            log.warn("Не удалось отправить уведомление получателю {}: {}", request.loginRecipient(), e.getMessage());
        }
    }

}
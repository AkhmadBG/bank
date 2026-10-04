package ru.practicum.transfer.service.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.feignclient.NotificationFeignClient;
import ru.practicum.interaction.notification.dto.NotificationRequest;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.transfer.config.CurrentUserProvider;
import ru.practicum.transfer.entity.TransferOperation;
import ru.practicum.transfer.exception.SelfTransferException;
import ru.practicum.transfer.feignclient.AccountInternalFeignClient;
import ru.practicum.transfer.repository.TransferOperationRepository;
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

        transferBalance(senderLogin, request);

        persistOperation(senderLogin, request);

        notifySender(senderLogin, request);
        notifyRecipient(senderLogin, request);
    }

    @CircuitBreaker(name = "accounts")
    @Retry(name = "accounts")
    protected void transferBalance(String senderLogin, TransferOperationRequest request) {
        accountInternalFeignClient.transferBalance(
                new InternalTransferRequest(senderLogin, request.loginRecipient(), request.amount())
        );
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

    @CircuitBreaker(name = "notifications", fallbackMethod = "notifySenderFallback")
    protected void notifySender(String senderLogin, TransferOperationRequest request) {
        notificationFeignClient.sendNotification(new NotificationRequest(
                senderLogin, "Перевод получателю " + request.loginRecipient() + " на сумму " + request.amount()));
    }

    private void notifySenderFallback(String senderLogin, TransferOperationRequest request, Throwable t) {
        log.warn("Не удалось отправить уведомление отправителю {}: {}", senderLogin, t.getMessage());
    }

    @CircuitBreaker(name = "notifications", fallbackMethod = "notifyRecipientFallback")
    protected void notifyRecipient(String senderLogin, TransferOperationRequest request) {
        notificationFeignClient.sendNotification(new NotificationRequest(
                request.loginRecipient(), "Получен перевод от " + senderLogin + " на сумму " + request.amount()));
    }

    private void notifyRecipientFallback(String senderLogin, TransferOperationRequest request, Throwable t) {
        log.warn("Не удалось отправить уведомление получателю {}: {}", request.loginRecipient(), t.getMessage());
    }

}
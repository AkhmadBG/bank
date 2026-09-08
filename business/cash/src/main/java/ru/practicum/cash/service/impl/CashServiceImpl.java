package ru.practicum.cash.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.cash.entity.CashOperation;
import ru.practicum.cash.feignclient.AccountInternalFeignClient;
import ru.practicum.cash.repository.CashOperationRepository;
import ru.practicum.cash.config.CurrentUserProvider;
import ru.practicum.cash.service.CashService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.feignclient.NotificationFeignClient;
import ru.practicum.interaction.notification.dto.NotificationRequest;
import ru.practicum.interaction.cash.enums.CashAction;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashServiceImpl implements CashService {

    private final AccountInternalFeignClient accountInternalFeignClient;
    private final NotificationFeignClient notificationFeignClient;
    private final CashOperationRepository cashOperationRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void cashOperation(CashOperationRequest request) {
        String login = currentUserProvider.getCurrentLogin();

        // 1. Изменение баланса — источник истины, делается первым.
        // Если этот вызов упадёт (сеть, недостаток средств и т.д.),
        // исключение вылетит наружу, ничего лишнего в БД Cash не запишется.
        accountInternalFeignClient.changeBalance(
                new BalanceChangeRequest(login, request.cashAction(), request.amount())
        );

        // 2. Баланс уже изменён — фиксируем факт операции в своей истории.
        persistOperation(login, request);

        // 3. Уведомление — best-effort, не должно ронять уже выполненную операцию.
        sendNotificationBestEffort(login, request);
    }

    @Transactional
    protected void persistOperation(String login, CashOperationRequest request) {
        CashOperation operation = CashOperation.builder()
                .login(login)
                .cashOperationType(request.cashAction())
                .amount(request.amount())
                .operationDateTime(LocalDateTime.now())
                .build();

        cashOperationRepository.save(operation);
    }

    private void sendNotificationBestEffort(String login, CashOperationRequest request) {
        try {
            String description = request.cashAction() == CashAction.PUT
                    ? "Пополнение счёта на " + request.amount()
                    : "Снятие со счёта " + request.amount();

            notificationFeignClient.sendNotification(new NotificationRequest(login, description));
        } catch (Exception e) {
            log.warn("Не удалось отправить уведомление для {}: {}", login, e.getMessage());
        }
    }
}

package ru.practicum.front.service.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.front.exception.BankOperationException;
import ru.practicum.front.feignclient.NotificationsFeignClient;
import ru.practicum.front.service.FrontService;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.NotificationDto;
import ru.practicum.interaction.front.dto.ResultData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontServiceImpl implements FrontService {

    private final FrontGatewayService frontGatewayService;
    private final NotificationsFeignClient notificationsFeignClient;

    @Override
    public ResultData getAccount() {
        return fetchCurrentState(null, null);
    }

    @Override
    public ResultData editAccount(String name, LocalDate birthdate) {
        String error = null;
        String info = null;

        try {
            frontGatewayService.editAccount(name, birthdate);
            info = "Данные сохранены";

        } catch (BankOperationException e) {
            log.warn(
                    "Ошибка операции изменения аккаунта: {}",
                    e.getMessage()
            );
            error = e.getMessage();

        } catch (FeignException e) {
            log.error(
                    "Ошибка Feign при изменении аккаунта: status={}, method={}, url={}",
                    e.status(),
                    e.request().httpMethod(),
                    e.request().url()
            );
            error = "Сервис аккаунтов временно недоступен";

        } catch (CallNotPermittedException e) {
            log.error(
                    "Circuit Breaker отклонил изменение аккаунта: {}",
                    e.getMessage()
            );
            error = "Сервис аккаунтов временно недоступен";
        }

        return fetchCurrentState(error, info);
    }

    @Override
    public ResultData editCash(
            UUID operationId,
            int value,
            CashAction action
    ) {
        String error = null;
        String info = null;

        try {
            frontGatewayService.cashOperation(operationId, action, value);
            info = action == CashAction.PUT
                    ? "Счёт пополнен"
                    : "Средства сняты";

        } catch (BankOperationException e) {
            error = e.getMessage();

        } catch (FeignException e) {
            log.error(
                    "Ошибка Feign при операции с наличными: status={}, method={}, url={}, body={}",
                    e.status(),
                    e.request().httpMethod(),
                    e.request().url(),
                    e.contentUTF8(),
                    e
            );

            if (e.status() == 400) {
                error = "Недостаточно средств на счёте или операция отклонена";
            } else {
                error = "Сервис для работы с наличными временно недоступен";
            }
        } catch (CallNotPermittedException e) {
            log.error(
                    "Circuit Breaker отклонил запрос: {}",
                    e.getMessage()
            );
            error = "Сервис обналичивания временно недоступен";
        }

        return fetchCurrentState(error, info);
    }

    @Override
    public ResultData transfer(
            UUID operationId,
            int value,
            String recipientAccountLogin
    ) {
        String error = null;
        String info = null;

        try {
            frontGatewayService.transfer(
                    operationId,
                    value,
                    recipientAccountLogin
            );
            info = "Перевод выполнен";

        } catch (BankOperationException e) {
            log.warn("Ошибка операции: {}", e.getMessage());
            error = e.getMessage();

        } catch (FeignException e) {
            log.error(
                    "Ошибка Feign при переводе: status={}, method={}, url={}, body={}",
                    e.status(),
                    e.request().httpMethod(),
                    e.request().url(),
                    e.contentUTF8(),
                    e
            );

            if (e.status() == 400) {
                error = "Недостаточно средств на счёте или перевод отклонён";
            } else {
                error = "Сервис переводов временно недоступен";
            }
        } catch (CallNotPermittedException e) {
            log.error(
                    "Circuit Breaker отклонил запрос: {}",
                    e.getMessage()
            );
            error = "Сервис переводов временно недоступен";
        }

        return fetchCurrentState(error, info);
    }

    @Override
    public List<NotificationDto> getNotifications(String recipient) {
        try {
            return notificationsFeignClient.getNotifications(recipient);
        } catch (Exception e) {
            log.warn(
                    "Не удалось загрузить уведомления для пользователя {}",
                    recipient,
                    e
            );
            return List.of();
        }
    }

    private ResultData fetchCurrentState(String error, String info) {
        try {
            ResultData fresh = frontGatewayService.fetchAccount();

            List<String> errors = error != null
                    ? List.of(error)
                    : null;

            return new ResultData(
                    fresh.name(),
                    fresh.birthdate(),
                    fresh.sum(),
                    fresh.accounts(),
                    errors,
                    info
            );

        } catch (Exception e) {
            log.error("Не удалось получить данные аккаунта", e);

            return new ResultData(
                    null,
                    null,
                    null,
                    List.of(),
                    List.of("Не удалось загрузить данные аккаунта"),
                    null
            );
        }
    }

}
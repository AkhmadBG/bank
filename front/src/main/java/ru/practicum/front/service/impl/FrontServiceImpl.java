package ru.practicum.front.service.impl;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.front.exception.BankOperationException;
import ru.practicum.front.feignclient.GatewayAccountFeignClient;
import ru.practicum.front.feignclient.GatewayCashFeignClient;
import ru.practicum.front.feignclient.GatewayTransferFeignClient;
import ru.practicum.front.service.FrontService;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontServiceImpl implements FrontService {

    private final GatewayAccountFeignClient accountFeignClient;
    private final GatewayCashFeignClient cashFeignClient;
    private final GatewayTransferFeignClient transferFeignClient;

    @Override
    public ResultData getAccount() {
        return fetchCurrentState(null, null);
    }

    @Override
    public ResultData editAccount(String name, LocalDate birthdate) {
        String error = null;
        String info = null;
        try {
            accountFeignClient.editAccount(new EditAccountRequest(name, birthdate));
            info = "Данные сохранены";
        } catch (BankOperationException e) {
            error = e.getMessage();
        } catch (FeignException e) {
            error = "Сервис аккаунтов временно недоступен";
        }
        return fetchCurrentState(error, info);
    }

    @Override
    public ResultData editCash(int value, CashAction action) {
        String error = null;
        String info = null;
        try {
            cashFeignClient.cashOperation(new CashOperationRequest(action, BigDecimal.valueOf(value)));
            info = action == CashAction.PUT ? "Счёт пополнен" : "Средства сняты";
        } catch (BankOperationException e) {
            error = e.getMessage();
        } catch (FeignException e) {
            error = "Сервис обналичивания временно недоступен";
        }
        return fetchCurrentState(error, info);
    }

    @Override
    public ResultData transfer(int value, String recipientAccountLogin) {
        String error = null;
        String info = null;
        try {
            transferFeignClient.transferOperation(
                    new TransferOperationRequest(recipientAccountLogin, BigDecimal.valueOf(value)));
            info = "Перевод выполнен";
        } catch (BankOperationException e) {
            error = e.getMessage();
        } catch (FeignException e) {
            error = "Сервис переводов временно недоступен";
        }
        return fetchCurrentState(error, info);
    }

    private ResultData fetchCurrentState(String error, String info) {
        try {
            ResultData fresh = accountFeignClient.getAccount().getBody();
            List<String> errors = error != null ? List.of(error) : null;
            return new ResultData(fresh.name(), fresh.birthdate(), fresh.sum(), fresh.accounts(), errors, info);
        } catch (Exception e) {
            log.error("Не удалось получить данные аккаунта", e);
            return new ResultData(null, null, null, List.of(), List.of("Не удалось загрузить данные аккаунта"), null);
        }
    }

}
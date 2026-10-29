package ru.practicum.front.service.impl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.front.feignclient.GatewayAccountFeignClient;
import ru.practicum.front.feignclient.GatewayCashFeignClient;
import ru.practicum.front.feignclient.GatewayTransferFeignClient;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FrontGatewayService {

    private final GatewayAccountFeignClient accountFeignClient;
    private final GatewayCashFeignClient cashFeignClient;
    private final GatewayTransferFeignClient transferFeignClient;

    @CircuitBreaker(name = "gateway")
    public void editAccount(String name, LocalDate birthdate) {
        accountFeignClient.editAccount(
                new EditAccountRequest(name, birthdate)
        );
    }

    @CircuitBreaker(name = "gateway")
    public void cashOperation(
            UUID operationId,
            CashAction action,
            int value
    ) {
        cashFeignClient.cashOperation(
                operationId,
                new CashOperationRequest(
                        action,
                        BigDecimal.valueOf(value)
                )
        );
    }

    @CircuitBreaker(name = "gateway")
    public void transfer(
            UUID operationId,
            int value,
            String recipientAccountLogin
    ) {
        transferFeignClient.transferOperation(
                operationId,
                new TransferOperationRequest(
                        recipientAccountLogin,
                        BigDecimal.valueOf(value)
                )
        );
    }

    @CircuitBreaker(name = "gateway")
    public ResultData fetchAccount() {
        return accountFeignClient.getAccount().getBody();
    }

}
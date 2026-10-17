package ru.practicum.cash.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.feignclient.AccountInternalFeignClient;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountsClient {

    private final AccountInternalFeignClient accountInternalFeignClient;

    @Retry(name = "accounts")
    @CircuitBreaker(name = "accounts")
    public void changeBalance(UUID operationId, String login, CashAction action, BigDecimal amount) {
        log.info("Запрос в Accounts: operationId={}, login={}, action={}", operationId, login, action);
        accountInternalFeignClient.changeBalance(
                new BalanceChangeRequest(operationId, login, action, amount));
    }

}
package ru.practicum.transfer.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
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
    public void transferBalance(UUID operationId, String senderLogin, String recipientLogin, BigDecimal amount) {
        log.info("Запрос в Accounts: operationId={}, sender={}, recipient={}", operationId, senderLogin, recipientLogin);
        accountInternalFeignClient.transferBalance(
                new InternalTransferRequest(operationId, senderLogin, recipientLogin, amount));
    }

}
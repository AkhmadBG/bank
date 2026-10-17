package ru.practicum.accounts.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.entity.ProcessedOperation;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.IdempotencyConflictException;
import ru.practicum.accounts.exception.InsufficientFundsException;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.ProcessedOperationRepository;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountInternalServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProcessedOperationRepository processedOperationRepository;

    private AccountInternalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountInternalServiceImpl(
                accountRepository,
                processedOperationRepository
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void changeBalance_shouldRejectInvalidAmount(BigDecimal amount) {
        BalanceChangeRequest request = balanceRequest(
                UUID.randomUUID(),
                "alice",
                CashAction.PUT,
                amount
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.changeBalance(request)
        );

        verifyNoInteractions(accountRepository);
        verifyNoInteractions(processedOperationRepository);
    }

    static Stream<Arguments> invalidAmounts() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO),
                Arguments.of(new BigDecimal("-10.00")),
                Arguments.of((BigDecimal) null)
        );
    }

    @Test
    void changeBalance_shouldIncreaseBalanceWhenPuttingMoney() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        Account account = account("alice", "100.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, CashAction.PUT.name(), "alice", "", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account));

        service.changeBalance(balanceRequest(operationId, "alice", CashAction.PUT, amount));

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(account);
    }

    @Test
    void changeBalance_shouldDecreaseBalanceWhenGettingMoney() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("30.00");

        Account account = account("alice", "100.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, CashAction.GET.name(), "alice", "", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account));

        service.changeBalance(balanceRequest(operationId, "alice", CashAction.GET, amount));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
        verify(accountRepository).save(account);
    }

    @Test
    void changeBalance_shouldThrowWhenBalanceIsInsufficient() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        Account account = account("alice", "20.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, CashAction.GET.name(), "alice", "", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account));

        assertThrows(
                InsufficientFundsException.class,
                () -> service.changeBalance(balanceRequest(operationId, "alice", CashAction.GET, amount))
        );

        // баланс не изменился
        assertThat(account.getBalance()).isEqualByComparingTo("20.00");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void changeBalance_shouldNotChangeBalanceForRepeatedOperation() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        ProcessedOperation previous = mock(ProcessedOperation.class);

        when(processedOperationRepository.insertIfAbsent(
                operationId, CashAction.PUT.name(), "alice", "", amount))
                .thenReturn(0);
        when(processedOperationRepository.findById(operationId))
                .thenReturn(Optional.of(previous));
        when(previous.matches(CashAction.PUT.name(), "alice", "", amount))
                .thenReturn(true);

        BalanceChangeRequest request = balanceRequest(operationId, "alice", CashAction.PUT, amount);

        assertDoesNotThrow(() -> service.changeBalance(request));

        verify(processedOperationRepository).findById(operationId);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void changeBalance_shouldThrowWhenOperationIdIsUsedWithDifferentData() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");

        ProcessedOperation previous = mock(ProcessedOperation.class);

        when(processedOperationRepository.insertIfAbsent(
                operationId, CashAction.PUT.name(), "alice", "", amount))
                .thenReturn(0);
        when(processedOperationRepository.findById(operationId))
                .thenReturn(Optional.of(previous));
        when(previous.matches(CashAction.PUT.name(), "alice", "", amount))
                .thenReturn(false);

        assertThrows(
                IdempotencyConflictException.class,
                () -> service.changeBalance(balanceRequest(operationId, "alice", CashAction.PUT, amount))
        );

        verifyNoInteractions(accountRepository);
    }

    @Test
    void changeBalance_shouldThrowWhenAccountDoesNotExist() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, CashAction.PUT.name(), "unknown", "", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("unknown"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> service.changeBalance(balanceRequest(operationId, "unknown", CashAction.PUT, amount))
        );

        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferBalance_shouldDecreaseSenderAndIncreaseRecipient() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.00");

        Account sender = account("alice", "100.00");
        Account recipient = account("bob", "40.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, "TRANSFER", "alice", "bob", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(sender));
        when(accountRepository.findByLoginForUpdate("bob"))
                .thenReturn(Optional.of(recipient));

        service.transferBalance(transferRequest(operationId, "alice", "bob", amount));

        assertThat(sender.getBalance()).isEqualByComparingTo("75.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("65.00");

    }

    @Test
    void transferBalance_shouldThrowWhenSenderHasInsufficientFunds() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");

        Account sender = account("alice", "100.00");
        Account recipient = account("bob", "40.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, "TRANSFER", "alice", "bob", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(sender));
        when(accountRepository.findByLoginForUpdate("bob"))
                .thenReturn(Optional.of(recipient));

        assertThrows(
                InsufficientFundsException.class,
                () -> service.transferBalance(transferRequest(operationId, "alice", "bob", amount))
        );

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("40.00");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferBalance_shouldThrowWhenRecipientDoesNotExist() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.00");

        Account sender = account("alice", "100.00");

        when(processedOperationRepository.insertIfAbsent(
                operationId, "TRANSFER", "alice", "ghost", amount))
                .thenReturn(1);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(sender));
        when(accountRepository.findByLoginForUpdate("ghost"))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> service.transferBalance(transferRequest(operationId, "alice", "ghost", amount))
        );

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
    }

    private Account account(String login, String balance) {
        return Account.builder()
                .login(login)
                .name(login)
                .birthdate(LocalDate.of(1990, 1, 1))
                .balance(new BigDecimal(balance))
                .build();
    }

    private BalanceChangeRequest balanceRequest(
            UUID operationId,
            String login,
            CashAction action,
            BigDecimal amount) {

        return new BalanceChangeRequest(
                operationId,
                login,
                action,
                amount
        );
    }

    private InternalTransferRequest transferRequest(
            UUID operationId,
            String sender,
            String recipient,
            BigDecimal amount) {

        return new InternalTransferRequest(
                operationId,
                sender,
                recipient,
                amount
        );
    }

}
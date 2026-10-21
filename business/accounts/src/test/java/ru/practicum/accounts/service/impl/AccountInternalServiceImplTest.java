package ru.practicum.accounts.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.entity.OutboxEvent;
import ru.practicum.accounts.entity.ProcessedOperation;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.IdempotencyConflictException;
import ru.practicum.accounts.exception.InsufficientFundsException;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.OutboxEventRepository;
import ru.practicum.accounts.repository.ProcessedOperationRepository;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountInternalServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ProcessedOperationRepository processedOperationRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private AccountInternalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AccountInternalServiceImpl(
                accountRepository,
                processedOperationRepository,
                outboxEventRepository,
                objectMapper
        );
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void changeBalance_shouldRejectInvalidAmount(BigDecimal amount) {
        BalanceChangeRequest request = balanceRequest(
                UUID.randomUUID(), "alice", CashAction.PUT, amount);

        assertThrows(IllegalArgumentException.class, () -> service.changeBalance(request));

        verifyNoInteractions(accountRepository, processedOperationRepository, outboxEventRepository);
    }

    @Test
    void changeBalance_shouldRejectMissingOperationId() {
        BalanceChangeRequest request = balanceRequest(
                null, "alice", CashAction.PUT, new BigDecimal("10.00"));

        assertThrows(IllegalArgumentException.class, () -> service.changeBalance(request));

        verifyNoInteractions(accountRepository, processedOperationRepository, outboxEventRepository);
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
        stubNewOperation(operationId, CashAction.PUT.name(), "alice", "", amount);
        when(accountRepository.findByLoginForUpdate("alice")).thenReturn(Optional.of(account));

        service.changeBalance(balanceRequest(operationId, "alice", CashAction.PUT, amount));

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(account);
    }

    @Test
    void changeBalance_shouldDecreaseBalanceWhenGettingMoney() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("30.00");
        Account account = account("alice", "100.00");
        stubNewOperation(operationId, CashAction.GET.name(), "alice", "", amount);
        when(accountRepository.findByLoginForUpdate("alice")).thenReturn(Optional.of(account));

        service.changeBalance(balanceRequest(operationId, "alice", CashAction.GET, amount));

        assertThat(account.getBalance()).isEqualByComparingTo("70.00");
        verify(accountRepository).save(account);
    }

    @Test
    void changeBalance_shouldThrowWhenBalanceIsInsufficient() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        Account account = account("alice", "20.00");
        stubNewOperation(operationId, CashAction.GET.name(), "alice", "", amount);
        when(accountRepository.findByLoginForUpdate("alice")).thenReturn(Optional.of(account));

        assertThrows(InsufficientFundsException.class,
                () -> service.changeBalance(balanceRequest(operationId, "alice", CashAction.GET, amount)));

        assertThat(account.getBalance()).isEqualByComparingTo("20.00");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void changeBalance_shouldThrowWhenAccountDoesNotExist() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        stubNewOperation(operationId, CashAction.PUT.name(), "unknown", "", amount);
        when(accountRepository.findByLoginForUpdate("unknown")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.changeBalance(balanceRequest(operationId, "unknown", CashAction.PUT, amount)));

        verify(accountRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void changeBalance_shouldNotChangeBalanceForRepeatedOperation() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        stubRepeatedOperation(operationId, CashAction.PUT.name(), "alice", "", amount, true);

        BalanceChangeRequest request = balanceRequest(operationId, "alice", CashAction.PUT, amount);

        assertDoesNotThrow(() -> service.changeBalance(request));

        verify(processedOperationRepository).findById(operationId);
        verifyNoInteractions(accountRepository, outboxEventRepository);
    }

    @Test
    void changeBalance_shouldThrowWhenOperationIdIsUsedWithDifferentData() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        stubRepeatedOperation(operationId, CashAction.PUT.name(), "alice", "", amount, false);

        assertThrows(IdempotencyConflictException.class,
                () -> service.changeBalance(balanceRequest(operationId, "alice", CashAction.PUT, amount)));

        verifyNoInteractions(accountRepository, outboxEventRepository);
    }

    @Test
    void changeBalance_shouldSaveDepositEventToOutbox() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        stubNewOperation(operationId, CashAction.PUT.name(), "alice", "", amount);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account("alice", "100.00")));

        service.changeBalance(balanceRequest(operationId, "alice", CashAction.PUT, amount));

        OutboxEvent event = savedOutboxEvents(1).get(0);
        assertThat(event.getOperationId()).isEqualTo(operationId);
        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventType()).isEqualTo("DEPOSIT");
        assertThat(event.getAttempts()).isZero();
        assertThat(event.getCreatedAt()).isNotNull();
        assertThat(event.getPublishedAt()).isNull();

        NotificationMessage message = payloadOf(event);
        assertThat(message.eventId()).isNotNull();
        assertThat(message.login()).isEqualTo("alice");
        assertThat(message.type()).isEqualTo(NotificationType.DEPOSIT);
        assertThat(message.message()).isEqualTo("Пополнение счёта");
        assertThat(message.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void changeBalance_shouldSaveWithdrawEventToOutbox() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("30.00");
        stubNewOperation(operationId, CashAction.GET.name(), "alice", "", amount);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account("alice", "100.00")));

        service.changeBalance(balanceRequest(operationId, "alice", CashAction.GET, amount));

        OutboxEvent event = savedOutboxEvents(1).get(0);
        assertThat(event.getEventType()).isEqualTo("WITHDRAW");

        NotificationMessage message = payloadOf(event);
        assertThat(message.login()).isEqualTo("alice");
        assertThat(message.type()).isEqualTo(NotificationType.WITHDRAW);
        assertThat(message.message()).isEqualTo("Снятие средств со счёта");
        assertThat(message.amount()).isEqualByComparingTo("30.00");
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void transferBalance_shouldRejectInvalidAmount(BigDecimal amount) {
        InternalTransferRequest request = transferRequest(UUID.randomUUID(), "alice", "bob", amount);

        assertThrows(IllegalArgumentException.class, () -> service.transferBalance(request));

        verifyNoInteractions(accountRepository, processedOperationRepository, outboxEventRepository);
    }

    @Test
    void transferBalance_shouldLockAccountsInAlphabeticalOrder() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        stubNewOperation(operationId, "TRANSFER", "bob", "alice", amount);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account("alice", "100.00")));
        when(accountRepository.findByLoginForUpdate("bob"))
                .thenReturn(Optional.of(account("bob", "100.00")));

        service.transferBalance(transferRequest(operationId, "bob", "alice", amount));

        InOrder inOrder = inOrder(accountRepository);
        inOrder.verify(accountRepository).findByLoginForUpdate("alice");
        inOrder.verify(accountRepository).findByLoginForUpdate("bob");
    }

    @Test
    void transferBalance_shouldThrowWhenSenderHasInsufficientFunds() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");
        Account sender = account("alice", "100.00");
        Account recipient = account("bob", "40.00");
        stubNewOperation(operationId, "TRANSFER", "alice", "bob", amount);
        when(accountRepository.findByLoginForUpdate("alice")).thenReturn(Optional.of(sender));
        when(accountRepository.findByLoginForUpdate("bob")).thenReturn(Optional.of(recipient));

        assertThrows(InsufficientFundsException.class,
                () -> service.transferBalance(transferRequest(operationId, "alice", "bob", amount)));

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        assertThat(recipient.getBalance()).isEqualByComparingTo("40.00");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void transferBalance_shouldThrowWhenRecipientDoesNotExist() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.00");
        Account sender = account("alice", "100.00");
        stubNewOperation(operationId, "TRANSFER", "alice", "ghost", amount);
        when(accountRepository.findByLoginForUpdate("alice")).thenReturn(Optional.of(sender));
        when(accountRepository.findByLoginForUpdate("ghost")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> service.transferBalance(transferRequest(operationId, "alice", "ghost", amount)));

        assertThat(sender.getBalance()).isEqualByComparingTo("100.00");
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void transferBalance_shouldNotChangeBalancesForRepeatedOperation() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.00");
        stubRepeatedOperation(operationId, "TRANSFER", "alice", "bob", amount, true);

        assertDoesNotThrow(() -> service.transferBalance(transferRequest(operationId, "alice", "bob", amount)));

        verifyNoInteractions(accountRepository, outboxEventRepository);
    }

    @Test
    void transferBalance_shouldThrowWhenOperationIdIsUsedWithDifferentData() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.00");
        stubRepeatedOperation(operationId, "TRANSFER", "alice", "bob", amount, false);

        assertThrows(IdempotencyConflictException.class,
                () -> service.transferBalance(transferRequest(operationId, "alice", "bob", amount)));

        verifyNoInteractions(accountRepository, outboxEventRepository);
    }

    @Test
    void transferBalance_shouldSaveEventsForSenderAndRecipient() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("25.00");
        stubNewOperation(operationId, "TRANSFER", "alice", "bob", amount);
        when(accountRepository.findByLoginForUpdate("alice"))
                .thenReturn(Optional.of(account("alice", "100.00")));
        when(accountRepository.findByLoginForUpdate("bob"))
                .thenReturn(Optional.of(account("bob", "40.00")));

        service.transferBalance(transferRequest(operationId, "alice", "bob", amount));

        List<OutboxEvent> events = savedOutboxEvents(2);
        assertThat(events).allSatisfy(e -> {
            assertThat(e.getOperationId()).isEqualTo(operationId);
            assertThat(e.getEventType()).isEqualTo("TRANSFER");
            assertThat(e.getAttempts()).isZero();
            assertThat(e.getPublishedAt()).isNull();
        });
        assertThat(events.get(0).getEventId()).isNotEqualTo(events.get(1).getEventId());

        NotificationMessage toSender = messageFor(events, "alice");
        assertThat(toSender.type()).isEqualTo(NotificationType.TRANSFER);
        assertThat(toSender.message()).isEqualTo("Перевод пользователю bob");
        assertThat(toSender.amount()).isEqualByComparingTo("25.00");

        NotificationMessage toRecipient = messageFor(events, "bob");
        assertThat(toRecipient.type()).isEqualTo(NotificationType.TRANSFER);
        assertThat(toRecipient.message()).isEqualTo("Получен перевод от пользователя alice");
        assertThat(toRecipient.amount()).isEqualByComparingTo("25.00");

        assertThat(toSender.eventId()).isNotEqualTo(toRecipient.eventId());
    }

    private void stubNewOperation(UUID operationId, String kind, String login,
                                  String counterparty, BigDecimal amount) {
        when(processedOperationRepository.insertIfAbsent(operationId, kind, login, counterparty, amount))
                .thenReturn(1);
    }

    private void stubRepeatedOperation(UUID operationId, String kind, String login,
                                       String counterparty, BigDecimal amount, boolean sameData) {
        ProcessedOperation previous = mock(ProcessedOperation.class);
        when(processedOperationRepository.insertIfAbsent(operationId, kind, login, counterparty, amount))
                .thenReturn(0);
        when(processedOperationRepository.findById(operationId)).thenReturn(Optional.of(previous));
        when(previous.matches(kind, login, counterparty, amount)).thenReturn(sameData);
    }

    private List<OutboxEvent> savedOutboxEvents(int expectedCount) {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private NotificationMessage payloadOf(OutboxEvent event) {
        return objectMapper.readValue(event.getPayload(), NotificationMessage.class);
    }

    private NotificationMessage messageFor(List<OutboxEvent> events, String login) {
        return events.stream()
                .map(this::payloadOf)
                .filter(m -> login.equals(m.login()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Нет события для " + login));
    }

    private Account account(String login, String balance) {
        return Account.builder()
                .login(login)
                .name(login)
                .birthdate(LocalDate.of(1990, 1, 1))
                .balance(new BigDecimal(balance))
                .build();
    }

    private BalanceChangeRequest balanceRequest(UUID operationId, String login,
                                                CashAction action, BigDecimal amount) {
        return new BalanceChangeRequest(operationId, login, action, amount);
    }

    private InternalTransferRequest transferRequest(UUID operationId, String sender,
                                                    String recipient, BigDecimal amount) {
        return new InternalTransferRequest(operationId, sender, recipient, amount);
    }

}
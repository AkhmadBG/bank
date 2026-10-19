package ru.practicum.cash.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.config.CurrentUserProvider;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceImplTest {

    private static final String LOGIN = "test-user";

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private CashOperationRecorder operationRecorder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private CashServiceImpl cashService;

    @BeforeEach
    void setUp() {
        cashService = new CashServiceImpl(accountsClient, operationRecorder, currentUserProvider);
    }

    @Test
    void shouldDepositMoney() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);

        cashService.cashOperation(operationId, new CashOperationRequest(CashAction.PUT, amount));

        verify(accountsClient).changeBalance(operationId, LOGIN, CashAction.PUT, amount);
        verify(operationRecorder).record(operationId, LOGIN, CashAction.PUT, amount);
        verifyNoMoreInteractions(accountsClient, operationRecorder);
    }

    @Test
    void shouldWithdrawMoney() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("50.00");
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);

        cashService.cashOperation(operationId, new CashOperationRequest(CashAction.GET, amount));

        verify(accountsClient).changeBalance(operationId, LOGIN, CashAction.GET, amount);
        verify(operationRecorder).record(operationId, LOGIN, CashAction.GET, amount);
        verifyNoMoreInteractions(accountsClient, operationRecorder);
    }

    @Test
    void shouldCallAccountsBeforeRecordingOperation() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);

        cashService.cashOperation(operationId, new CashOperationRequest(CashAction.PUT, amount));

        InOrder inOrder = inOrder(accountsClient, operationRecorder);
        inOrder.verify(accountsClient).changeBalance(operationId, LOGIN, CashAction.PUT, amount);
        inOrder.verify(operationRecorder).record(operationId, LOGIN, CashAction.PUT, amount);
    }

    @Test
    void shouldPassClientOperationIdToAccountsAndRecorderUnchanged() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);

        cashService.cashOperation(operationId, new CashOperationRequest(CashAction.PUT, amount));

        verify(accountsClient).changeBalance(eq(operationId), any(), any(), any());
        verify(operationRecorder).record(eq(operationId), any(), any(), any());
    }

    @Test
    void repeatedRequestWithSameKeyIsSentToAccountsWithSameKey() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        CashOperationRequest request = new CashOperationRequest(CashAction.GET, amount);
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);

        cashService.cashOperation(operationId, request);
        cashService.cashOperation(operationId, request);

        verify(accountsClient, times(2)).changeBalance(operationId, LOGIN, CashAction.GET, amount);
    }

    @Test
    void retryAfterAccountsFailureUsesSameKey() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("10.00");
        CashOperationRequest request = new CashOperationRequest(CashAction.PUT, amount);
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);
        doThrow(new RuntimeException("timeout"))
                .doNothing()
                .when(accountsClient).changeBalance(operationId, LOGIN, CashAction.PUT, amount);

        assertThatThrownBy(() -> cashService.cashOperation(operationId, request))
                .isInstanceOf(RuntimeException.class);
        cashService.cashOperation(operationId, request);

        verify(accountsClient, times(2)).changeBalance(operationId, LOGIN, CashAction.PUT, amount);
        verify(operationRecorder, times(1)).record(operationId, LOGIN, CashAction.PUT, amount);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void shouldRejectInvalidAmount(BigDecimal amount) {
        CashOperationRequest request = new CashOperationRequest(CashAction.PUT, amount);

        assertThatThrownBy(() -> cashService.cashOperation(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(accountsClient, operationRecorder, currentUserProvider);
    }

    static Stream<Arguments> invalidAmounts() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO),
                Arguments.of(new BigDecimal("-10.00")),
                Arguments.of((BigDecimal) null)
        );
    }

    @Test
    void shouldNotRecordOperationWhenAccountsClientFails() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);
        doThrow(new RuntimeException("Accounts service unavailable"))
                .when(accountsClient).changeBalance(operationId, LOGIN, CashAction.PUT, amount);

        assertThatThrownBy(() ->
                cashService.cashOperation(operationId, new CashOperationRequest(CashAction.PUT, amount)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Accounts service unavailable");

        verifyNoInteractions(operationRecorder);
    }

    @Test
    void shouldPropagateRecorderFailureAfterBalanceWasChanged() {
        UUID operationId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);
        doThrow(new RuntimeException("Recording failed"))
                .when(operationRecorder).record(operationId, LOGIN, CashAction.PUT, amount);

        assertThatThrownBy(() ->
                cashService.cashOperation(operationId, new CashOperationRequest(CashAction.PUT, amount)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recording failed");

        verify(accountsClient).changeBalance(operationId, LOGIN, CashAction.PUT, amount);
    }

}
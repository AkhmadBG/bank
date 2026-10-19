package ru.practicum.transfer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.config.CurrentUserProvider;
import ru.practicum.transfer.exception.SelfTransferException;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    private static final String SENDER = "sender";
    private static final String RECIPIENT = "recipient";
    private static final BigDecimal AMOUNT = new BigDecimal("100.50");

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private TransferOperationRecorder operationRecorder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private TransferServiceImpl transferService;

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(accountsClient, currentUserProvider, operationRecorder);
    }

    @Test
    void transfer_success() {
        UUID operationId = UUID.randomUUID();
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        transferService.transfer(operationId, new TransferOperationRequest(RECIPIENT, AMOUNT));

        verify(accountsClient).transferBalance(operationId, SENDER, RECIPIENT, AMOUNT);
        verifyNoMoreInteractions(accountsClient);
    }

    @Test
    void transfer_shouldUseSenderFromCurrentUserNotFromRequest() {
        UUID operationId = UUID.randomUUID();
        when(currentUserProvider.getCurrentLogin()).thenReturn("current-user");

        transferService.transfer(operationId, new TransferOperationRequest(RECIPIENT, AMOUNT));

        verify(accountsClient).transferBalance(operationId, "current-user", RECIPIENT, AMOUNT);
    }

    @Test
    void transfer_shouldPassClientOperationIdToAccountsUnchanged() {
        UUID operationId = UUID.randomUUID();
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        transferService.transfer(operationId, new TransferOperationRequest(RECIPIENT, AMOUNT));

        verify(accountsClient).transferBalance(eq(operationId), any(), any(), any());
    }

    @Test
    void transfer_repeatedRequestWithSameKeyIsSentToAccountsWithSameKey() {
        UUID operationId = UUID.randomUUID();
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        transferService.transfer(operationId, request);
        transferService.transfer(operationId, request);

        verify(accountsClient, times(2)).transferBalance(operationId, SENDER, RECIPIENT, AMOUNT);
    }

    @Test
    void transfer_retryAfterAccountsFailureUsesSameKey() {
        UUID operationId = UUID.randomUUID();
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);
        doThrow(new RuntimeException("timeout"))
                .doNothing()
                .when(accountsClient).transferBalance(operationId, SENDER, RECIPIENT, AMOUNT);

        assertThatThrownBy(() -> transferService.transfer(operationId, request))
                .isInstanceOf(RuntimeException.class);
        transferService.transfer(operationId, request);

        verify(accountsClient, times(2)).transferBalance(operationId, SENDER, RECIPIENT, AMOUNT);
    }

    @Test
    void transfer_shouldThrowSelfTransfer_whenSenderEqualsRecipient() {
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        assertThatThrownBy(() ->
                transferService.transfer(UUID.randomUUID(), new TransferOperationRequest(SENDER, AMOUNT)))
                .isInstanceOf(SelfTransferException.class);

        verifyNoInteractions(accountsClient);
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void transfer_shouldRejectInvalidAmount(BigDecimal amount) {
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, amount);

        assertThatThrownBy(() -> transferService.transfer(UUID.randomUUID(), request))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(accountsClient, currentUserProvider);
    }

    static Stream<Arguments> invalidAmounts() {
        return Stream.of(
                Arguments.of(BigDecimal.ZERO),
                Arguments.of(new BigDecimal("-10.00")),
                Arguments.of((BigDecimal) null)
        );
    }

    @Test
    void transfer_shouldPropagateAccountsClientFailure() {
        UUID operationId = UUID.randomUUID();
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);
        doThrow(new RuntimeException("boom"))
                .when(accountsClient).transferBalance(operationId, SENDER, RECIPIENT, AMOUNT);

        assertThatThrownBy(() ->
                transferService.transfer(operationId, new TransferOperationRequest(RECIPIENT, AMOUNT)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");
    }

}
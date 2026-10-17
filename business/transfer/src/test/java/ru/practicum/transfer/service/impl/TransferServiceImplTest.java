package ru.practicum.transfer.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationPublisher;
import ru.practicum.interaction.kafka.NotificationType;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.interaction.util.ApplicationUtil;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.config.CurrentUserProvider;
import ru.practicum.transfer.exception.SelfTransferException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private AccountsClient accountsClient;
    @Mock
    private TransferOperationRecorder operationRecorder;
    @Mock
    private NotificationPublisher notificationPublisher;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private TransferServiceImpl transferService;

    private static final String SENDER = "sender";
    private static final String RECIPIENT = "recipient";
    private static final BigDecimal AMOUNT = new BigDecimal("100.50");

    @BeforeEach
    void setUp() {
        transferService = new TransferServiceImpl(
                accountsClient, operationRecorder, notificationPublisher, currentUserProvider);
    }

    @Test
    void transfer_success() {
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        transferService.transfer(request);

        verify(currentUserProvider).getCurrentLogin();
        verify(accountsClient).transferBalance(
                any(UUID.class), eq(SENDER), eq(RECIPIENT), eq(AMOUNT));
        verify(operationRecorder).record(SENDER, RECIPIENT, AMOUNT);
        verify(notificationPublisher, times(2)).publish(any(NotificationMessage.class));
        verifyNoMoreInteractions(accountsClient, operationRecorder);
    }

    @Test
    void transfer_shouldThrowSelfTransfer_whenSenderEqualsRecipient() {
        TransferOperationRequest request = new TransferOperationRequest(SENDER, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(SelfTransferException.class);

        verifyNoInteractions(accountsClient, operationRecorder, notificationPublisher);
    }

    @Test
    void transfer_shouldValidateAmount() {
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        try (MockedStatic<ApplicationUtil> util = mockStatic(ApplicationUtil.class)) {
            transferService.transfer(request);

            util.verify(() -> ApplicationUtil.validateAmount(AMOUNT));
        }
    }

    @Test
    void transfer_shouldPublishNotificationToSenderAndRecipient() {
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        transferService.transfer(request);

        ArgumentCaptor<NotificationMessage> captor =
                ArgumentCaptor.forClass(NotificationMessage.class);
        verify(notificationPublisher, times(2)).publish(captor.capture());
        List<NotificationMessage> messages = captor.getAllValues();

        NotificationMessage senderMsg = messages.stream()
                .filter(m -> SENDER.equals(m.login()))
                .findFirst().orElseThrow();
        NotificationMessage recipientMsg = messages.stream()
                .filter(m -> RECIPIENT.equals(m.login()))
                .findFirst().orElseThrow();

        assertThat(senderMsg.type()).isEqualTo(NotificationType.TRANSFER);
        assertThat(senderMsg.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(senderMsg.message()).contains(RECIPIENT);

        assertThat(recipientMsg.type()).isEqualTo(NotificationType.TRANSFER);
        assertThat(recipientMsg.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(recipientMsg.message()).contains(SENDER);
    }

    @Test
    void transfer_shouldCallAccountsClientBeforeRecorder() {
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);

        transferService.transfer(request);

        InOrder inOrder = inOrder(accountsClient, operationRecorder, notificationPublisher);
        inOrder.verify(accountsClient).transferBalance(any(), eq(SENDER), eq(RECIPIENT), eq(AMOUNT));
        inOrder.verify(operationRecorder).record(SENDER, RECIPIENT, AMOUNT);
        inOrder.verify(notificationPublisher, times(2)).publish(any(NotificationMessage.class));
    }

    @Test
    void transfer_shouldNotRecordOrNotify_whenTransferBalanceFails() {
        TransferOperationRequest request = new TransferOperationRequest(RECIPIENT, AMOUNT);
        when(currentUserProvider.getCurrentLogin()).thenReturn(SENDER);
        doThrow(new RuntimeException("boom"))
                .when(accountsClient).transferBalance(any(), eq(SENDER), eq(RECIPIENT), eq(AMOUNT));

        assertThatThrownBy(() -> transferService.transfer(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("boom");

        verify(operationRecorder, never()).record(any(), any(), any());
        verifyNoInteractions(notificationPublisher);
    }

}
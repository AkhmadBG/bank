package ru.practicum.cash.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.cash.client.AccountsClient;
import ru.practicum.cash.config.CurrentUserProvider;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationPublisher;
import ru.practicum.interaction.kafka.NotificationType;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashServiceImplTest {

    private static final String LOGIN = "test-user";

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private CashOperationRecorder operationRecorder;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private CashServiceImpl cashService;

    @BeforeEach
    void setUp() {
        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);
    }

    @Test
    void shouldDepositMoneyAndSendNotification() {
        BigDecimal amount = new BigDecimal("100.00");
        CashOperationRequest request = new CashOperationRequest(
                CashAction.PUT, amount
        );

        cashService.cashOperation(request);

        ArgumentCaptor<UUID> operationIdCaptor =
                ArgumentCaptor.forClass(UUID.class);

        verify(accountsClient).changeBalance(
                operationIdCaptor.capture(),
                eq(LOGIN),
                eq(CashAction.PUT),
                eq(amount)
        );

        assertNotNull(operationIdCaptor.getValue());

        verify(operationRecorder).record(LOGIN, CashAction.PUT, amount);

        ArgumentCaptor<NotificationMessage> notificationCaptor =
                ArgumentCaptor.forClass(NotificationMessage.class);

        verify(notificationPublisher).publish(notificationCaptor.capture());

        NotificationMessage notification = notificationCaptor.getValue();

        assertNotNull(notification);
        assertEquals(LOGIN, notification.login());
        assertEquals(NotificationType.DEPOSIT, notification.type());
        assertEquals("Пополнение счёта на " + amount, notification.message());
        assertEquals(amount, notification.amount());

        InOrder inOrder = inOrder(accountsClient, operationRecorder,
                notificationPublisher);

        inOrder.verify(accountsClient).changeBalance(
                any(UUID.class), eq(LOGIN), eq(CashAction.PUT), eq(amount)
        );
        inOrder.verify(operationRecorder).record(LOGIN, CashAction.PUT, amount);
        inOrder.verify(notificationPublisher).publish(any(NotificationMessage.class));
    }

    @Test
    void shouldWithdrawMoneyAndSendNotification() {
        BigDecimal amount = new BigDecimal("50.00");
        CashOperationRequest request = new CashOperationRequest(
                CashAction.GET, amount
        );

        cashService.cashOperation(request);

        verify(accountsClient).changeBalance(
                any(UUID.class), eq(LOGIN), eq(CashAction.GET), eq(amount)
        );

        verify(operationRecorder).record(LOGIN, CashAction.GET, amount);

        ArgumentCaptor<NotificationMessage> notificationCaptor =
                ArgumentCaptor.forClass(NotificationMessage.class);

        verify(notificationPublisher).publish(notificationCaptor.capture());

        NotificationMessage notification = notificationCaptor.getValue();

        assertEquals(LOGIN, notification.login());
        assertEquals(NotificationType.WITHDRAW, notification.type());
        assertEquals("Снятие со счёта " + amount, notification.message());
        assertEquals(amount, notification.amount());
    }

    @Test
    void shouldNotRecordOperationOrNotifyWhenAccountsClientFails() {
        BigDecimal amount = new BigDecimal("100.00");
        CashOperationRequest request = new CashOperationRequest(
                CashAction.PUT, amount
        );

        doThrow(new RuntimeException("Accounts service unavailable"))
                .when(accountsClient)
                .changeBalance(
                        any(UUID.class),
                        eq(LOGIN),
                        eq(CashAction.PUT),
                        eq(amount)
                );

        assertThrows(
                RuntimeException.class,
                () -> cashService.cashOperation(request)
        );

        verify(accountsClient).changeBalance(
                any(UUID.class), eq(LOGIN), eq(CashAction.PUT), eq(amount)
        );

        verifyNoInteractions(operationRecorder);
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    void shouldNotNotifyWhenRecordingOperationFails() {
        BigDecimal amount = new BigDecimal("100.00");
        CashOperationRequest request = new CashOperationRequest(
                CashAction.PUT, amount
        );

        doThrow(new RuntimeException("Recording failed"))
                .when(operationRecorder)
                .record(LOGIN, CashAction.PUT, amount);

        assertThrows(
                RuntimeException.class,
                () -> cashService.cashOperation(request)
        );

        verify(accountsClient).changeBalance(
                any(UUID.class), eq(LOGIN), eq(CashAction.PUT), eq(amount)
        );

        verify(operationRecorder).record(LOGIN, CashAction.PUT, amount);
        verifyNoInteractions(notificationPublisher);
    }

    @Test
    void shouldPropagateNotificationPublishingFailure() {
        BigDecimal amount = new BigDecimal("100.00");
        CashOperationRequest request = new CashOperationRequest(
                CashAction.PUT, amount
        );

        doThrow(new RuntimeException("Kafka unavailable"))
                .when(notificationPublisher)
                .publish(any(NotificationMessage.class));

        assertThrows(
                RuntimeException.class,
                () -> cashService.cashOperation(request)
        );

        verify(accountsClient).changeBalance(
                any(UUID.class), eq(LOGIN), eq(CashAction.PUT), eq(amount)
        );
        verify(operationRecorder).record(LOGIN, CashAction.PUT, amount);
        verify(notificationPublisher).publish(any(NotificationMessage.class));
    }

}
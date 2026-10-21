package ru.practicum.notifications.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.notifications.service.NotificationService;

import static org.mockito.Mockito.*;

class NotificationListenerTest {

    private NotificationService notificationService;
    private NotificationListener notificationListener;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        notificationListener = new NotificationListener(notificationService);
    }

    @Test
    void onMessageShouldPassMessageToNotificationService() {
        NotificationMessage message = mock(NotificationMessage.class);

        notificationListener.onMessage(message);

        verify(notificationService).send(message);
    }

    @Test
    void onMessageShouldPassSameMessageOnlyOnce() {
        NotificationMessage message = mock(NotificationMessage.class);

        notificationListener.onMessage(message);

        verify(notificationService, times(1)).send(message);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    void onMessageShouldPropagateServiceException() {
        NotificationMessage message = mock(NotificationMessage.class);

        doThrow(new IllegalStateException("База данных недоступна"))
                .when(notificationService).send(message);

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> notificationListener.onMessage(message)
        );
    }

}
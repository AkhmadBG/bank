package ru.practicum.notifications.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationType;
import ru.practicum.notifications.entity.Notification;
import ru.practicum.notifications.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    private SimpleMeterRegistry meterRegistry;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                meterRegistry
        );
    }

    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    @Test
    void shouldSaveNotificationWhenEventIsNew() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение счёта на 100.00",
                null
        );

        when(notificationRepository.existsByEventId(eventId))
                .thenReturn(false);

        notificationService.send(message);

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertEquals(eventId, saved.getEventId());
        assertEquals("test-user", saved.getRecipient());
        assertEquals("Пополнение счёта на 100.00", saved.getDescription());
        assertNotNull(saved.getCreatedAt());

        verify(notificationRepository).existsByEventId(eventId);
    }

    @Test
    void shouldSkipNotificationWhenEventWasAlreadyProcessed() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение счёта на 100.00",
                null
        );

        when(notificationRepository.existsByEventId(eventId))
                .thenReturn(true);

        notificationService.send(message);

        verify(notificationRepository).existsByEventId(eventId);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSaveNotificationWhenEventIdIsNull() {
        NotificationMessage message = new NotificationMessage(
                null,
                "test-user",
                NotificationType.WITHDRAW,
                "Снятие со счёта 50.00",
                null
        );

        notificationService.send(message);

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();

        assertNull(saved.getEventId());
        assertEquals("test-user", saved.getRecipient());
        assertEquals("Снятие со счёта 50.00", saved.getDescription());
        assertNotNull(saved.getCreatedAt());

        verify(notificationRepository, never()).existsByEventId(any());
    }

    @Test
    void shouldSetCreationTimeWhenSavingNotification() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.ACCOUNT_UPDATE,
                "Данные аккаунта изменены",
                null
        );

        LocalDateTime before = LocalDateTime.now();

        notificationService.send(message);

        LocalDateTime after = LocalDateTime.now();

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository).save(captor.capture());

        LocalDateTime createdAt = captor.getValue().getCreatedAt();

        assertNotNull(createdAt);
        assertFalse(createdAt.isBefore(before));
        assertFalse(createdAt.isAfter(after));
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenCheckingDuplicate() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение счёта на 100.00",
                null
        );

        when(notificationRepository.existsByEventId(eventId))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> notificationService.send(message)
        );

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldPropagateRepositoryExceptionWhenSavingNotification() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение счёта на 100.00",
                null
        );

        when(notificationRepository.existsByEventId(eventId))
                .thenReturn(false);

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> notificationService.send(message)
        );

        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void shouldIncrementFailureMetricWhenRepositoryThrowsException() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение счёта на 100.00",
                null
        );

        when(notificationRepository.existsByEventId(eventId))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> notificationService.send(message)
        );

        Counter counter = meterRegistry.find("notification.failures")
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void shouldSaveNotificationsWithDifferentEventIds() {
        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();

        NotificationMessage firstMessage = new NotificationMessage(
                firstEventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение на 100.00",
                null
        );

        NotificationMessage secondMessage = new NotificationMessage(
                secondEventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение на 200.00",
                null
        );

        when(notificationRepository.existsByEventId(firstEventId))
                .thenReturn(false);
        when(notificationRepository.existsByEventId(secondEventId))
                .thenReturn(false);

        notificationService.send(firstMessage);
        notificationService.send(secondMessage);

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);

        verify(notificationRepository, times(2)).save(captor.capture());

        assertEquals(
                firstEventId,
                captor.getAllValues().get(0).getEventId()
        );
        assertEquals(
                secondEventId,
                captor.getAllValues().get(1).getEventId()
        );
    }

    @Test
    void shouldIncrementFailureMetricWhenSavingNotificationFails() {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.DEPOSIT,
                "Пополнение счёта на 100.00",
                null
        );

        when(notificationRepository.existsByEventId(eventId))
                .thenReturn(false);

        when(notificationRepository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database unavailable"));

        assertThrows(
                RuntimeException.class,
                () -> notificationService.send(message)
        );

        Counter counter = meterRegistry.find("notification.failures")
                .counter();

        assertNotNull(counter);
        assertEquals(1.0, counter.count());

        verify(notificationRepository).existsByEventId(eventId);
        verify(notificationRepository).save(any(Notification.class));
    }

}
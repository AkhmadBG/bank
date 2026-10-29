package ru.practicum.accounts.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.accounts.entity.OutboxEvent;
import ru.practicum.accounts.repository.OutboxEventRepository;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        outboxPublisher = new OutboxPublisher(
                outboxEventRepository,
                objectMapper,
                kafkaTemplate
        );
    }

    @Test
    void shouldRetrySendingEventAfterKafkaRecovers() {
        UUID eventId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                "test-user",
                NotificationType.ACCOUNT_UPDATE,
                "Данные аккаунта изменены",
                null
        );

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);
        event.setOperationId(operationId);
        event.setEventType(NotificationType.ACCOUNT_UPDATE.name());
        event.setPayload(objectMapper.writeValueAsString(message));
        event.setCreatedAt(LocalDateTime.now());
        event.setAttempts(0);

        when(outboxEventRepository
                .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc())
                .thenReturn(List.of(event));

        when(kafkaTemplate.send(
                eq("notifications"),
                eq("test-user"),
                any(NotificationMessage.class)
        )).thenReturn(
                CompletableFuture.failedFuture(
                        new RuntimeException("Kafka недоступна")
                ),
                CompletableFuture.completedFuture(null)
        );

        assertDoesNotThrow(() -> outboxPublisher.publishPendingEvents());

        assertNull(event.getPublishedAt());
        assertEquals(1, event.getAttempts());

        assertDoesNotThrow(() -> outboxPublisher.publishPendingEvents());

        assertNotNull(event.getPublishedAt());
        assertEquals(1, event.getAttempts());

        verify(kafkaTemplate, times(2)).send(
                eq("notifications"),
                eq("test-user"),
                any(NotificationMessage.class)
        );

        verify(outboxEventRepository, times(2)).save(event);
    }

}
package ru.practicum.interaction.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationPublisherTest {

    private KafkaTemplate<String, NotificationMessage> kafkaTemplate;
    private NotificationPublisher publisher;
    private NotificationMessage message;

    private CompletableFuture<SendResult<String, NotificationMessage>> future;

    @BeforeEach
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new NotificationPublisher(kafkaTemplate);
        message = mock(NotificationMessage.class);
        future = mock(CompletableFuture.class);

        when(message.login()).thenReturn("test-user");
        when(message.type()).thenReturn(null);
    }

    @AfterEach
    void clearInterruptFlag() {
        // Не позволяем прерыванию одного теста повлиять на следующие.
        Thread.interrupted();
    }

    @Test
    void publishShouldSendMessageSuccessfully() throws Exception {
        when(kafkaTemplate.send(
                KafkaTopics.NOTIFICATIONS, "test-user", message
        )).thenReturn(future);

        when(future.get(15L, TimeUnit.SECONDS)).thenReturn(null);

        assertDoesNotThrow(() -> publisher.publish(message));

        verify(kafkaTemplate).send(
                KafkaTopics.NOTIFICATIONS, "test-user", message
        );
        verify(future).get(15L, TimeUnit.SECONDS);
    }

    @Test
    void publishShouldThrowIllegalStateExceptionWhenKafkaFails()
            throws Exception {
        when(kafkaTemplate.send(
                KafkaTopics.NOTIFICATIONS, "test-user", message
        )).thenReturn(future);

        when(future.get(15L, TimeUnit.SECONDS))
                .thenThrow(new ExecutionException(
                        new RuntimeException("Kafka unavailable")
                ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publisher.publish(message)
        );

        assertEquals("Не удалось отправить сообщение", exception.getMessage());
        assertInstanceOf(ExecutionException.class, exception.getCause());
    }

    @Test
    void publishShouldThrowIllegalStateExceptionWhenTimeoutOccurs()
            throws Exception {
        when(kafkaTemplate.send(
                KafkaTopics.NOTIFICATIONS, "test-user", message
        )).thenReturn(future);

        when(future.get(15L, TimeUnit.SECONDS))
                .thenThrow(new TimeoutException("Kafka response timeout"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publisher.publish(message)
        );

        assertEquals("Не удалось отправить сообщение", exception.getMessage());
        assertInstanceOf(TimeoutException.class, exception.getCause());
    }

    @Test
    void publishShouldRestoreInterruptFlagWhenInterrupted()
            throws Exception {
        when(kafkaTemplate.send(
                KafkaTopics.NOTIFICATIONS, "test-user", message
        )).thenReturn(future);

        when(future.get(15L, TimeUnit.SECONDS))
                .thenThrow(new InterruptedException("Thread interrupted"));

        try {
            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> publisher.publish(message)
            );

            assertEquals(
                    "Отправка уведомления была прервана",
                    exception.getMessage()
            );
            assertInstanceOf(
                    InterruptedException.class,
                    exception.getCause()
            );
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void publishShouldThrowIllegalStateExceptionWhenSendFailsImmediately() {
        when(kafkaTemplate.send(
                KafkaTopics.NOTIFICATIONS, "test-user", message
        )).thenThrow(new RuntimeException("Cannot connect to Kafka"));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> publisher.publish(message)
        );

        assertEquals("Не удалось отправить сообщение", exception.getMessage());
        assertInstanceOf(RuntimeException.class, exception.getCause());
    }

}
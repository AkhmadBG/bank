package ru.practicum.interaction.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class NotificationPublisher {

    private static final long SEND_TIMEOUT_SECONDS = 15;

    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    public NotificationPublisher(KafkaTemplate<String, NotificationMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Отправляет уведомление и ждёт подтверждения брокера.
     * Ошибки только логируются: сбой уведомления не должен ломать бизнес-операцию.
     */
    public void publish(NotificationMessage message) {
        try {
            kafkaTemplate.send(KafkaTopics.NOTIFICATIONS, message.login(), message)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.debug("Notification sent: login={}, type={}", message.login(), message.type());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending notification for {}", message.login(), e);
        } catch (Exception e) {
            log.error("Failed to send notification for {} (type {})", message.login(), message.type(), e);
        }
    }

}
package ru.practicum.accounts.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.practicum.accounts.entity.OutboxEvent;
import ru.practicum.accounts.repository.OutboxEventRepository;
import ru.practicum.interaction.kafka.KafkaTopics;
import ru.practicum.interaction.kafka.NotificationMessage;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, NotificationMessage> kafkaTemplate;

    @Scheduled(fixedDelay = 2000)
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxEventRepository
                        .findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

        for (OutboxEvent event : events) {
            try {
                NotificationMessage message = objectMapper.readValue(
                        event.getPayload(),
                        NotificationMessage.class
                );

                kafkaTemplate.send(
                        KafkaTopics.NOTIFICATIONS,
                        message.login(),
                        message
                ).get(15, TimeUnit.SECONDS);

                markPublished(event);
            } catch (Exception e) {
                log.warn(
                        "Не удалось отправить событие {}. Повторим позже",
                        event.getEventId(),
                        e
                );

                incrementAttempts(event);
            }
        }
    }

    private void markPublished(OutboxEvent event) {
        event.setPublishedAt(java.time.LocalDateTime.now());
        outboxEventRepository.save(event);
    }

    private void incrementAttempts(OutboxEvent event) {
        event.setAttempts(event.getAttempts() + 1);
        outboxEventRepository.save(event);
    }

}
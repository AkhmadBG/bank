package ru.practicum.notifications;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.interaction.kafka.KafkaTopics;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationPublisher;
import ru.practicum.interaction.kafka.NotificationType;
import ru.practicum.notifications.repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaTopics.NOTIFICATIONS, "notifications.DLT"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@Import(NotificationPublisher.class)
class NotificationPublisherToConsumerIntegrationTest {

    @TestConfiguration
    static class ProducerConfiguration {

        @Bean
        KafkaTemplate<String, NotificationMessage> notificationKafkaTemplate(
                @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
            JsonSerializer<NotificationMessage> valueSerializer = new JsonSerializer<>();
            valueSerializer.setAddTypeInfo(false);   // spring.json.add.type.headers: false

            Map<String, Object> props = Map.of(
                    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                    ProducerConfig.ACKS_CONFIG, "all");
            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(
                    props, new StringSerializer(), valueSerializer));
        }
    }

    @Autowired
    private NotificationPublisher publisher;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void eventPublishedByRealPublisherIsSavedByConsumer() {
        UUID eventId = UUID.randomUUID();

        publisher.publish(new NotificationMessage(
                eventId, "petrov", NotificationType.DEPOSIT,
                "Пополнение счёта на 10", new BigDecimal("10.00")));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var saved = notificationRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getEventId()).isEqualTo(eventId);
            assertThat(saved.get(0).getRecipient()).isEqualTo("petrov");
            assertThat(saved.get(0).getDescription()).isEqualTo("Пополнение счёта на 10");
        });
    }

    @Test
    void sameEventPublishedTwiceIsSavedOnce() {
        var message = new NotificationMessage(UUID.randomUUID(), "petrov",
                NotificationType.WITHDRAW, "dup", new BigDecimal("5.00"));

        publisher.publish(message);
        publisher.publish(message);
        publisher.publish(new NotificationMessage(UUID.randomUUID(), "petrov",
                NotificationType.TRANSFER, "sentinel", BigDecimal.ONE));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(count("sentinel")).isEqualTo(1));
        assertThat(count("dup")).isEqualTo(1);
    }

    private long count(String description) {
        return notificationRepository.findAll().stream()
                .filter(n -> description.equals(n.getDescription()))
                .count();
    }

}
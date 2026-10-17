package ru.practicum.notifications;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.interaction.kafka.KafkaTopics;
import ru.practicum.notifications.repository.NotificationRepository;

import java.time.Duration;
import java.util.List;
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
class KafkaConsumerErrorHandlingIntegrationTest {

    private static final String DLT = "notifications.DLT";

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private NotificationRepository notificationRepository;

    private KafkaTemplate<String, String> rawTemplate;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        Map<String, Object> props = Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString());
        rawTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
    }

    @Test
    void brokenMessageGoesToDltAndConsumerKeepsWorking() {
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "broken", "this is not json");

        awaitInDlt("this is not json");

        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", json(UUID.randomUUID(), "after-broken"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(countByDescription("after-broken")).isEqualTo(1));
    }

    @Test
    void duplicateEventIsSavedOnlyOnce() {
        UUID eventId = UUID.randomUUID();
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", json(eventId, "dup"));
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", json(eventId, "dup"));
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", json(UUID.randomUUID(), "sentinel"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(countByDescription("sentinel")).isEqualTo(1));

        assertThat(countByDescription("dup")).isEqualTo(1);
    }

    private long countByDescription(String description) {
        return notificationRepository.findAll().stream()
                .filter(n -> description.equals(n.getDescription()))
                .count();
    }

    private static String json(UUID eventId, String message) {
        return """
                {"eventId":"%s","login":"petrov","type":"DEPOSIT","message":"%s","amount":1}
                """.formatted(eventId, message);
    }

    private void awaitInDlt(String expectedValue) {
        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of(DLT));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : consumer.poll(Duration.ofMillis(500))) {
                    if (expectedValue.equals(record.value())) {
                        return;
                    }
                }
            }
        }
        throw new AssertionError("Сообщение не найдено в топике " + DLT);
    }

}
package ru.practicum.notifications;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.interaction.kafka.KafkaTopics;
import ru.practicum.notifications.repository.NotificationRepository;

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
class KafkaConsumerIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker broker;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private KafkaListenerEndpointRegistry listenerRegistry;

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
    void messageFromKafkaIsSavedToDatabase() {
        UUID eventId = UUID.randomUUID();
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", json(eventId, "Пополнение счёта на 10"));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            var saved = notificationRepository.findAll();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getRecipient()).isEqualTo("petrov");
            assertThat(saved.get(0).getDescription()).isEqualTo("Пополнение счёта на 10");
            assertThat(saved.get(0).getEventId()).isEqualTo(eventId);
        });
    }

    @Test
    void consumerResumesFromLastCommittedOffsetAfterRestart() {
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", jsonWithoutEventId("before-restart"));
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(count("before-restart")).isEqualTo(1));

        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::stop);
        rawTemplate.send(KafkaTopics.NOTIFICATIONS, "petrov", jsonWithoutEventId("while-stopped"));
        listenerRegistry.getListenerContainers().forEach(MessageListenerContainer::start);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(count("while-stopped")).isEqualTo(1));
        assertThat(count("before-restart")).isEqualTo(1);
    }

    private long count(String description) {
        return notificationRepository.findAll().stream()
                .filter(n -> description.equals(n.getDescription()))
                .count();
    }

    private static String json(UUID eventId, String message) {
        return """
                {"eventId":"%s","login":"petrov","type":"DEPOSIT","message":"%s","amount":10}
                """.formatted(eventId, message);
    }

    private static String jsonWithoutEventId(String message) {
        return """
                {"login":"petrov","type":"DEPOSIT","message":"%s","amount":10}
                """.formatted(message);
    }

}
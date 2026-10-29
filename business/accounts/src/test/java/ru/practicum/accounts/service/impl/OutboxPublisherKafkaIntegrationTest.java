package ru.practicum.accounts.service.impl;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.practicum.accounts.service.AccountInternalService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.kafka.KafkaTopics;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationType;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = "KEYCLOAK_ACCOUNTS_CLIENT_SECRET=test-secret")
@EmbeddedKafka(
        partitions = 1,
        topics = KafkaTopics.NOTIFICATIONS,
        bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OutboxPublisherKafkaIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresConfig {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }

    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    AccountInternalService service;
    @Autowired
    DataSource dataSource;

    @Test
    void depositEventTravelsFromOutboxToKafka() {
        UUID operationId = UUID.randomUUID();

        service.changeBalance(new BalanceChangeRequest(
                operationId, "kozlov", CashAction.PUT, new BigDecimal("25.00")));

        NotificationMessage received = awaitMessage("kozlov", NotificationType.DEPOSIT);

        assertThat(received.eventId()).isNotNull();
        assertThat(received.amount()).isEqualByComparingTo("25.00");

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(jdbc.queryForObject(
                        "SELECT count(*) FROM outbox_event WHERE operation_id = ? AND published_at IS NOT NULL",
                        Long.class, operationId)).isEqualTo(1));
    }

    @Test
    void transferEventsAreDeliveredToSenderAndRecipient() {
        service.transferBalance(new InternalTransferRequest(
                UUID.randomUUID(), "sidorova", "ivanov", new BigDecimal("40.00")));

        NotificationMessage toSender = awaitMessage("sidorova", NotificationType.TRANSFER);
        NotificationMessage toRecipient = awaitMessage("ivanov", NotificationType.TRANSFER);

        assertThat(toSender.message()).contains("ivanov");
        assertThat(toRecipient.message()).contains("sidorova");
    }

    private NotificationMessage awaitMessage(String login, NotificationType type) {
        JsonDeserializer<NotificationMessage> valueDeserializer =
                new JsonDeserializer<>(NotificationMessage.class, false);
        valueDeserializer.addTrustedPackages("ru.practicum.interaction.kafka");

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, broker.getBrokersAsString(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (Consumer<String, NotificationMessage> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), valueDeserializer).createConsumer()) {
            consumer.subscribe(List.of(KafkaTopics.NOTIFICATIONS));
            long deadline = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, NotificationMessage> record : consumer.poll(Duration.ofMillis(500))) {
                    NotificationMessage value = record.value();
                    if (value != null && login.equals(record.key()) && value.type() == type) {
                        return value;
                    }
                }
            }
        }
        throw new AssertionError("Событие %s для %s не найдено в топике".formatted(type, login));
    }

}
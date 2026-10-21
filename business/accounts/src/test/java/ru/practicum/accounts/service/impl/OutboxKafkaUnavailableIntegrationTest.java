package ru.practicum.accounts.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.practicum.accounts.service.AccountInternalService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.cash.enums.CashAction;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "KEYCLOAK_ACCOUNTS_CLIENT_SECRET=test-secret",
        "spring.kafka.bootstrap-servers=localhost:1"})
@Testcontainers
class OutboxKafkaUnavailableIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    AccountInternalService service;
    @Autowired
    DataSource dataSource;

    @Test
    void operationSucceedsAndEventStaysInOutboxWhileKafkaIsDown() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID operationId = UUID.randomUUID();

        service.changeBalance(new BalanceChangeRequest(
                operationId, "kozlov", CashAction.PUT, new BigDecimal("100.00")));

        assertThat(jdbc.queryForObject("SELECT balance FROM accounts WHERE login = 'kozlov'",
                BigDecimal.class)).isEqualByComparingTo("20100.00");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT published_at, attempts FROM outbox_event WHERE operation_id = ?", operationId);
            assertThat(row.get("published_at")).isNull();
            assertThat(((Number) row.get("attempts")).intValue()).isGreaterThanOrEqualTo(1);
        });
    }

}
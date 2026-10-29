package ru.practicum.accounts.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.IdempotencyConflictException;
import ru.practicum.accounts.exception.InsufficientFundsException;
import ru.practicum.accounts.repository.ProcessedOperationRepository;
import ru.practicum.accounts.service.AccountInternalService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationType;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "KEYCLOAK_ACCOUNTS_CLIENT_SECRET=test-secret")
@Testcontainers
class AccountInternalServicePostgresIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @MockitoBean
    OutboxPublisher outboxPublisher;

    @Autowired
    AccountInternalService service;
    @Autowired
    ProcessedOperationRepository processedRepository;
    @Autowired
    PlatformTransactionManager transactionManager;
    @Autowired
    DataSource dataSource;
    @Autowired
    ObjectMapper objectMapper;

    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM outbox_event");
        jdbc.update("DELETE FROM processed_operation");
        jdbc.update("UPDATE accounts SET balance = 10000.00 WHERE login = 'petrov'");
        jdbc.update("UPDATE accounts SET balance = 5000.00 WHERE login = 'ivanov'");
    }

    @Test
    void insertIfAbsentReturnsOneThenZeroForSameKey() {
        UUID id = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Integer first = tx.execute(s ->
                processedRepository.insertIfAbsent(id, "GET", "petrov", "", new BigDecimal("10.00")));
        Integer second = tx.execute(s ->
                processedRepository.insertIfAbsent(id, "GET", "petrov", "", new BigDecimal("10.00")));

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(processedCount()).isEqualTo(1);
    }

    @Test
    void repeatedWithdrawChangesBalanceOnce() {
        BalanceChangeRequest request = withdraw(UUID.randomUUID(), "petrov", "10.00");

        service.changeBalance(request);
        service.changeBalance(request);

        assertThat(balanceOf("petrov")).isEqualByComparingTo("9990.00");
        assertThat(processedCount()).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    void outboxPayloadContainsNotificationForOperation() throws Exception {
        service.changeBalance(withdraw(UUID.randomUUID(), "petrov", "10.00"));

        String payload = jdbc.queryForObject("SELECT payload FROM outbox_event", String.class);
        NotificationMessage message = objectMapper.readValue(payload, NotificationMessage.class);

        assertThat(message.login()).isEqualTo("petrov");
        assertThat(message.type()).isEqualTo(NotificationType.WITHDRAW);
        assertThat(message.amount()).isEqualByComparingTo("10.00");
        assertThat(message.eventId()).isNotNull();
    }

    @Test
    void sameKeyWithDifferentAmountIsConflict() {
        UUID id = UUID.randomUUID();
        service.changeBalance(withdraw(id, "petrov", "10.00"));

        assertThrows(IdempotencyConflictException.class,
                () -> service.changeBalance(withdraw(id, "petrov", "20.00")));

        assertThat(balanceOf("petrov")).isEqualByComparingTo("9990.00");
    }

    @Test
    void sameKeyWithDifferentActionIsConflict() {
        UUID id = UUID.randomUUID();
        service.changeBalance(withdraw(id, "petrov", "10.00"));

        assertThrows(IdempotencyConflictException.class,
                () -> service.changeBalance(deposit(id, "petrov", "10.00")));

        assertThat(balanceOf("petrov")).isEqualByComparingTo("9990.00");
    }

    @Test
    void parallelRequestsWithSameKeyApplyOnce() throws Exception {
        BalanceChangeRequest request =
                withdraw(UUID.randomUUID(), "petrov", "10.00");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new java.util.ArrayList<>();

        boolean completedSuccessfully = false;

        try {
            futures = IntStream.range(0, 8)
                    .mapToObj(i -> pool.submit(
                            () -> service.changeBalance(request)
                    ))
                    .collect(Collectors.toList());

            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            completedSuccessfully = true;
        } finally {
            if (!completedSuccessfully) {
                for (Future<?> future : futures) {
                    future.cancel(true);
                }
                pool.shutdownNow();
            } else {
                pool.shutdown();
            }

            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        assertThat(balanceOf("petrov"))
                .isEqualByComparingTo("9990.00");
        assertThat(processedCount()).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    void repeatedTransferChangesBalancesOnce() {
        InternalTransferRequest request = transfer(UUID.randomUUID(), "petrov", "ivanov", "30.00");

        service.transferBalance(request);
        service.transferBalance(request);

        assertThat(balanceOf("petrov")).isEqualByComparingTo("9970.00");
        assertThat(balanceOf("ivanov")).isEqualByComparingTo("5030.00");
        assertThat(outboxCount()).isEqualTo(2);   // отправителю и получателю
    }

    @Test
    void insufficientFundsRollsBackProcessedOperationAndOutbox() {
        UUID id = UUID.randomUUID();

        assertThrows(InsufficientFundsException.class,
                () -> service.changeBalance(withdraw(id, "petrov", "20000.00")));

        assertThat(balanceOf("petrov")).isEqualByComparingTo("10000.00");
        assertThat(processedCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    @Test
    void transferToMissingRecipientRollsBackEverything() {
        assertThrows(AccountNotFoundException.class,
                () -> service.transferBalance(transfer(UUID.randomUUID(), "petrov", "nobody", "10.00")));

        assertThat(balanceOf("petrov")).isEqualByComparingTo("10000.00");
        assertThat(processedCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    @Test
    void retryWithSameKeyAfterRollbackSucceeds() {
        UUID id = UUID.randomUUID();
        assertThrows(InsufficientFundsException.class,
                () -> service.changeBalance(withdraw(id, "petrov", "10500.00")));

        service.changeBalance(deposit(UUID.randomUUID(), "petrov", "1000.00"));
        service.changeBalance(withdraw(id, "petrov", "10500.00"));   // тот же ключ, теперь денег хватает

        assertThat(balanceOf("petrov")).isEqualByComparingTo("500.00");
    }

    @Test
    void outboxInsertFailureRollsBackBalanceAndProcessedOperation() {
        jdbc.execute("""
                CREATE OR REPLACE FUNCTION fail_outbox_insert() RETURNS trigger AS $$
                BEGIN RAISE EXCEPTION 'outbox failure'; END;
                $$ LANGUAGE plpgsql""");
        jdbc.execute("""
                CREATE TRIGGER trg_fail_outbox BEFORE INSERT ON outbox_event
                FOR EACH ROW EXECUTE FUNCTION fail_outbox_insert()""");
        try {
            assertThrows(RuntimeException.class,
                    () -> service.changeBalance(deposit(UUID.randomUUID(), "petrov", "50.00")));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS trg_fail_outbox ON outbox_event");
        }

        assertThat(balanceOf("petrov")).isEqualByComparingTo("10000.00");
        assertThat(processedCount()).isZero();
        assertThat(outboxCount()).isZero();
    }

    private BigDecimal balanceOf(String login) {
        return jdbc.queryForObject("SELECT balance FROM accounts WHERE login = ?", BigDecimal.class, login);
    }

    private long processedCount() {
        return jdbc.queryForObject("SELECT count(*) FROM processed_operation", Long.class);
    }

    private long outboxCount() {
        return jdbc.queryForObject("SELECT count(*) FROM outbox_event", Long.class);
    }

    private BalanceChangeRequest withdraw(UUID id, String login, String amount) {
        return new BalanceChangeRequest(id, login, CashAction.GET, new BigDecimal(amount));
    }

    private BalanceChangeRequest deposit(UUID id, String login, String amount) {
        return new BalanceChangeRequest(id, login, CashAction.PUT, new BigDecimal(amount));
    }

    private InternalTransferRequest transfer(UUID id, String from, String to, String amount) {
        return new InternalTransferRequest(id, from, to, new BigDecimal(amount));
    }

}
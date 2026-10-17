package ru.practicum.accounts.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.accounts.entity.ProcessedOperation;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProcessedOperationRepository extends JpaRepository<ProcessedOperation, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_operation (operation_id, kind, login, counterparty, amount)
            VALUES (:operationId, :kind, :login, :counterparty, :amount)
            ON CONFLICT (operation_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("operationId") UUID operationId,
                       @Param("kind") String kind,
                       @Param("login") String login,
                       @Param("counterparty") String counterparty,
                       @Param("amount") BigDecimal amount);
}
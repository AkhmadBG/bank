package ru.practicum.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.cash.entity.CashOperation;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO cash_operations
                (operation_id, login, cash_operation_type, amount, operation_date_time)
            VALUES
                (:operationId, :login, :action, :amount, CURRENT_TIMESTAMP)
            ON CONFLICT (operation_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("operationId") UUID operationId,
            @Param("login") String login,
            @Param("action") String action,
            @Param("amount") BigDecimal amount
    );

}
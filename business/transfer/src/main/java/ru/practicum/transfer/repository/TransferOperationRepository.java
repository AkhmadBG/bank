package ru.practicum.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.transfer.entity.TransferOperation;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO transfer_operations
                (operation_id, login_sender, login_recipient, amount, operation_date_time)
            VALUES
                (:operationId, :senderLogin, :recipientLogin, :amount, CURRENT_TIMESTAMP)
            ON CONFLICT (operation_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("operationId") UUID operationId,
            @Param("senderLogin") String senderLogin,
            @Param("recipientLogin") String recipientLogin,
            @Param("amount") BigDecimal amount
    );

}
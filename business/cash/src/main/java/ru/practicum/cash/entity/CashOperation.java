package ru.practicum.cash.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "cashOperationId")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "cash_operations")
public class CashOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cash_operation_id")
    private Long cashOperationId;

    @Column(name = "login", nullable = false)
    private String login;

    @Enumerated(EnumType.STRING)
    @Column(name = "cash_operation_type", nullable = false, length = 50)
    private CashAction cashOperationType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "operation_date_time", nullable = false)
    private LocalDateTime operationDateTime;

}
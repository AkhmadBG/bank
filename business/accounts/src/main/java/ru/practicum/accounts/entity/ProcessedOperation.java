package ru.practicum.accounts.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "processed_operation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedOperation {

    @Id
    @Column(name = "operation_id")
    private UUID operationId;

    @Column(name = "kind", nullable = false, length = 16)
    private String kind;

    @Column(name = "login", nullable = false)
    private String login;

    @Column(name = "counterparty", nullable = false)
    private String counterparty;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    public boolean matches(String kind, String login, String counterparty, BigDecimal amount) {
        return this.kind.equals(kind)
                && this.login.equals(login)
                && this.counterparty.equals(counterparty)
                && this.amount.compareTo(amount) == 0;
    }

}
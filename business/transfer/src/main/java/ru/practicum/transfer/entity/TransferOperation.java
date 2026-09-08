package ru.practicum.transfer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@EqualsAndHashCode(of = "transferOperationId")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "transfer_operations")
public class TransferOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_operation_id")
    private Long transferOperationId;

    @NotNull
    @Column(name = "login_recipient", nullable = false, length = 150)
    private String loginRecipient;

    @NotNull
    @Column(name = "login_sender", nullable = false, length = 150)
    private String loginSender;

    @NotNull
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @NotNull
    @Column(name = "operation_date_time", nullable = false)
    private LocalDateTime operationDateTime;

}
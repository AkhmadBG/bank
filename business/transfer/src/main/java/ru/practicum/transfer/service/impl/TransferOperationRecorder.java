package ru.practicum.transfer.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.transfer.entity.TransferOperation;
import ru.practicum.transfer.repository.TransferOperationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TransferOperationRecorder {

    private final TransferOperationRepository transferOperationRepository;

    @Transactional
    public void record(String senderLogin, String recipientLogin, BigDecimal amount) {
        TransferOperation operation = TransferOperation.builder()
                .loginSender(senderLogin)
                .loginRecipient(recipientLogin)
                .amount(amount)
                .operationDateTime(LocalDateTime.now())
                .build();

        transferOperationRepository.save(operation);
    }

}
package ru.practicum.transfer.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.transfer.repository.TransferOperationRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TransferOperationRecorder {

    private final TransferOperationRepository transferOperationRepository;

    @Transactional
    public boolean record(
            UUID operationId,
            String senderLogin,
            String recipientLogin,
            BigDecimal amount
    ) {
        int inserted = transferOperationRepository.insertIfAbsent(
                operationId,
                senderLogin,
                recipientLogin,
                amount
        );

        return inserted == 1;
    }

}
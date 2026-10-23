
package ru.practicum.transfer.service.impl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.interaction.util.ApplicationUtil;
import ru.practicum.transfer.client.AccountsClient;
import ru.practicum.transfer.config.CurrentUserProvider;
import ru.practicum.transfer.exception.SelfTransferException;
import ru.practicum.transfer.service.TransferService;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private final AccountsClient accountsClient;
    private final CurrentUserProvider currentUserProvider;
    private final TransferOperationRecorder transferOperationRecorder;
    private final MeterRegistry meterRegistry;

    @Override
    public void transfer(UUID operationId, TransferOperationRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        String senderLogin = currentUserProvider.getCurrentLogin();
        String recipientLogin = request.loginRecipient();

        if (senderLogin.equals(recipientLogin)) {
            throw new SelfTransferException();
        }

        try {
            accountsClient.transferBalance(
                    operationId,
                    senderLogin,
                    recipientLogin,
                    request.amount()
            );
        } catch (RuntimeException e) {
            meterRegistry.counter(
                    "transfer.failures",
                    "sender", senderLogin,
                    "recipient", recipientLogin
            ).increment();

            log.warn(
                    "Transfer failed: sender={}, recipient={}",
                    senderLogin,
                    recipientLogin
            );
            throw e;
        }

        transferOperationRecorder.record(
                operationId,
                senderLogin,
                recipientLogin,
                request.amount()
        );
    }

}
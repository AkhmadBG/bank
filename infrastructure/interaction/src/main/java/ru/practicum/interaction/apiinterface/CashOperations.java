package ru.practicum.interaction.apiinterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.practicum.interaction.cash.dto.CashOperationRequest;

import java.util.UUID;

public interface CashOperations {

    @PostMapping
    ResponseEntity<Void> cashOperation(@RequestHeader("Idempotency-Key") UUID operationId,
                                       @RequestBody CashOperationRequest cashOperationRequest);

}
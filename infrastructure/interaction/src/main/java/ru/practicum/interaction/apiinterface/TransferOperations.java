package ru.practicum.interaction.apiinterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

import java.util.UUID;

public interface TransferOperations {

    @PostMapping
    ResponseEntity<Void> transferOperation(@RequestHeader("Idempotency-Key") UUID operationId,
                                           @RequestBody TransferOperationRequest transferOperationRequest);

}
package ru.practicum.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.interaction.apiinterface.TransferOperations;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.transfer.service.TransferService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfer")
public class TransferController implements TransferOperations {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transferOperation(@RequestHeader("Idempotency-Key") UUID operationId,
            @Valid @RequestBody TransferOperationRequest request) {
        transferService.transfer(operationId, request);
        return ResponseEntity.ok().build();
    }

}
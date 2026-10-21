package ru.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.cash.service.CashService;
import ru.practicum.interaction.apiinterface.CashOperations;
import ru.practicum.interaction.cash.dto.CashOperationRequest;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cash")
public class CashController implements CashOperations {

    private final CashService cashService;

    @PostMapping
    public ResponseEntity<Void> cashOperation(@RequestHeader("Idempotency-Key") UUID operationId,
                                              @Valid @RequestBody CashOperationRequest request) {
        cashService.cashOperation(operationId, request);
        return ResponseEntity.ok().build();
    }

}
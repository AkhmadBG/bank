package ru.practicum.cash.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.cash.service.CashService;
import ru.practicum.interaction.apiinterface.CashOperations;
import ru.practicum.interaction.cash.dto.CashOperationRequest;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cash")
public class CashController implements CashOperations {

    private final CashService cashService;

    @PostMapping
    public ResponseEntity<Void> cashOperation(@RequestBody @Valid CashOperationRequest cashOperationRequest) {
        cashService.cashOperation(cashOperationRequest);
        return ResponseEntity.ok().build();
    }

}
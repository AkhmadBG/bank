package ru.practicum.accounts.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.accounts.service.AccountInternalService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.apiinterface.AccountInternalOperations;

@RestController
@RequiredArgsConstructor
public class AccountInternalController implements AccountInternalOperations {

    private final AccountInternalService accountInternalService;

    @PostMapping("/internal/account/balance")
    public ResponseEntity<Void> changeBalance(@Valid @RequestBody BalanceChangeRequest request) {
        accountInternalService.changeBalance(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/internal/account/transfer")
    public ResponseEntity<Void> transferBalance(@Valid @RequestBody InternalTransferRequest request) {
        accountInternalService.transferBalance(request);
        return ResponseEntity.ok().build();
    }

}
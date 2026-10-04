package ru.practicum.accounts.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.accounts.service.AccountService;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.apiinterface.AccountOperations;
import ru.practicum.interaction.front.dto.ResultData;

@RestController
@RequiredArgsConstructor
@RequestMapping("/account")
public class AccountsController implements AccountOperations {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ResultData> getAccount() {
        ResultData result = accountService.getAccount();
        return ResponseEntity.ok(result);
    }

    @PatchMapping
    public ResponseEntity<ResultData> editAccount(@RequestBody EditAccountRequest editAccountRequest) {
        ResultData result = accountService.editAccount(editAccountRequest);
        return ResponseEntity.ok(result);
    }

}
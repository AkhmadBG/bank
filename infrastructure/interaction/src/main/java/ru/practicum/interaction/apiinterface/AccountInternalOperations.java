package ru.practicum.interaction.apiinterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;

public interface AccountInternalOperations {

    @PostMapping("/internal/account/balance")
    ResponseEntity<Void> changeBalance(@RequestBody BalanceChangeRequest request);

    @PostMapping("/internal/account/transfer")
    ResponseEntity<Void> transferBalance(@RequestBody InternalTransferRequest request);

}
package ru.practicum.interaction.apiinterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.cash.dto.CashOperationRequest;

public interface CashOperations {

    @PostMapping
    ResponseEntity<Void> cashOperation(@RequestBody CashOperationRequest cashOperationRequest);

}
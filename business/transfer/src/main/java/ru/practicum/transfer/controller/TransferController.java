package ru.practicum.transfer.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.interaction.apiinterface.TransferOperations;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;
import ru.practicum.transfer.service.TransferService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/transfer")
public class TransferController implements TransferOperations {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<Void> transferOperation(@RequestBody @Valid TransferOperationRequest transferOperationRequest) {
        transferService.transfer(transferOperationRequest);
        return ResponseEntity.ok().build();
    }

}
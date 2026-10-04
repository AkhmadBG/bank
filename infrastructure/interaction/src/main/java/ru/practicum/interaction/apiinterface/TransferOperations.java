package ru.practicum.interaction.apiinterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

public interface TransferOperations {

    @PostMapping
    ResponseEntity<Void> transferOperation(@RequestBody TransferOperationRequest transferOperationRequest);

}
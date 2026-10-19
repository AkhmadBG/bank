package ru.practicum.transfer.service;

import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

import java.util.UUID;

public interface TransferService {

    void transfer(UUID operationId, TransferOperationRequest request);

}
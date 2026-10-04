package ru.practicum.transfer.service;

import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

public interface TransferService {

    void transfer(TransferOperationRequest request);

}
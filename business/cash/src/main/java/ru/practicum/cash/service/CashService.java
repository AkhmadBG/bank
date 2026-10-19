package ru.practicum.cash.service;

import ru.practicum.interaction.cash.dto.CashOperationRequest;

import java.util.UUID;

public interface CashService {

    void cashOperation(UUID operationId, CashOperationRequest cashOperationRequest);

}
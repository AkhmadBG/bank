package ru.practicum.cash.service;

import ru.practicum.interaction.cash.dto.CashOperationRequest;

public interface CashService {

    void cashOperation(CashOperationRequest cashOperationRequest);

}
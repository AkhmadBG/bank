package ru.practicum.cash.service;

import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.front.dto.ResultData;

public interface CashService {

    void cashOperation(CashOperationRequest cashOperationRequest);

}
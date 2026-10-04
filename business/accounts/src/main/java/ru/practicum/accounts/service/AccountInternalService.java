package ru.practicum.accounts.service;

import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;

public interface AccountInternalService {

    void changeBalance(BalanceChangeRequest request);

    void transferBalance(InternalTransferRequest request);

}
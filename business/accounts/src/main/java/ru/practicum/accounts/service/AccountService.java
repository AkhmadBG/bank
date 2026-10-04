package ru.practicum.accounts.service;

import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.front.dto.ResultData;

public interface AccountService {

    ResultData getAccount();

    ResultData editAccount(EditAccountRequest editAccountRequest);

}
package ru.practicum.front.service;

import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.ResultData;

import java.time.LocalDate;

public interface FrontService {

    ResultData getAccount();

    ResultData editAccount(String name, LocalDate birthdate);

    ResultData editCash(int value, CashAction action);

    ResultData transfer(int value, String recipientAccountLogin);

}
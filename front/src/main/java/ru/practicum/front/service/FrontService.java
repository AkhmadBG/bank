package ru.practicum.front.service;

import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.NotificationDto;
import ru.practicum.interaction.front.dto.ResultData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FrontService {

    ResultData getAccount();

    ResultData editAccount(String name, LocalDate birthdate);

    ResultData editCash(UUID operationId, int value, CashAction action);

    ResultData transfer(UUID operationId,int value, String recipientAccountLogin);

    List<NotificationDto> getNotifications(String recipient);

}
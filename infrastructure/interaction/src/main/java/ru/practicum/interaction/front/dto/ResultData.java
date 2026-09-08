package ru.practicum.interaction.front.dto;

import ru.practicum.interaction.account.dto.UserAccountDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResultData(

        String name,

        LocalDate birthdate,

        BigDecimal sum,

        List<UserAccountDto> accounts,

        List<String> errors,

        String info

) {
}
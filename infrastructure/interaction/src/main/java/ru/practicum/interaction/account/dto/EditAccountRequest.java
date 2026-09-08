package ru.practicum.interaction.account.dto;

import java.time.LocalDate;

public record EditAccountRequest(

        String name,

        LocalDate birthdate

) {
}
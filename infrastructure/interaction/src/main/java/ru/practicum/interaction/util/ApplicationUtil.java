package ru.practicum.interaction.util;

import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

@UtilityClass
public class ApplicationUtil {

    public void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля");
        }
    }

}
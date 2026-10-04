package ru.practicum.accounts.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(String login, BigDecimal balance, BigDecimal requested) {
        super("Недостаточно средств на счёте " + login + ": баланс " + balance + ", запрошено " + requested);
    }

}
package ru.practicum.accounts.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.InsufficientFundsException;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.service.AccountInternalService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.cash.enums.CashAction;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountInternalServiceImpl implements AccountInternalService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void changeBalance(BalanceChangeRequest request) {
        Account account = accountRepository.findByLoginForUpdate(request.login())
                .orElseThrow(() -> new AccountNotFoundException(request.login()));

        applyDelta(account, request.cashAction(), request.amount());
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void transferBalance(InternalTransferRequest request) {
        // Блокируем оба счёта в детерминированном порядке (по login),
        // чтобы встречные переводы не привели к deadlock на уровне БД
        String first = request.loginSender().compareTo(request.loginRecipient()) < 0
                ? request.loginSender() : request.loginRecipient();
        String second = request.loginSender().compareTo(request.loginRecipient()) < 0
                ? request.loginRecipient() : request.loginSender();

        Account firstLocked = accountRepository.findByLoginForUpdate(first)
                .orElseThrow(() -> new AccountNotFoundException(first));
        Account secondLocked = accountRepository.findByLoginForUpdate(second)
                .orElseThrow(() -> new AccountNotFoundException(second));

        Account sender = firstLocked.getLogin().equals(request.loginSender()) ? firstLocked : secondLocked;
        Account recipient = firstLocked.getLogin().equals(request.loginSender()) ? secondLocked : firstLocked;

        applyDelta(sender, CashAction.GET, request.amount());
        applyDelta(recipient, CashAction.PUT, request.amount());

        accountRepository.save(sender);
        accountRepository.save(recipient);
    }

    private void applyDelta(Account account, CashAction action, BigDecimal amount) {
        if (action == CashAction.GET) {
            if (account.getBalance().compareTo(amount) < 0) {
                throw new InsufficientFundsException(account.getLogin(), account.getBalance(), amount);
            }
            account.setBalance(account.getBalance().subtract(amount));
        } else {
            account.setBalance(account.getBalance().add(amount));
        }
    }

}
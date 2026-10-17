package ru.practicum.accounts.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.entity.ProcessedOperation;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.IdempotencyConflictException;
import ru.practicum.accounts.exception.InsufficientFundsException;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.ProcessedOperationRepository;
import ru.practicum.accounts.service.AccountInternalService;
import ru.practicum.interaction.account.dto.BalanceChangeRequest;
import ru.practicum.interaction.account.dto.InternalTransferRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.util.ApplicationUtil;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountInternalServiceImpl implements AccountInternalService {

    private final AccountRepository accountRepository;
    private final ProcessedOperationRepository processedOperationRepository;
    private static final String KIND_TRANSFER = "TRANSFER";

    @Override
    @Transactional
    public void changeBalance(BalanceChangeRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        ApplicationUtil.validateOperationId(request.operationId());
        if (isRepeatedOperation(request.operationId(), request.cashAction().name(),
                request.login(), "", request.amount())) {
            return;
        }
        Account account = accountRepository.findByLoginForUpdate(request.login())
                .orElseThrow(() -> new AccountNotFoundException(request.login()));

        applyDelta(account, request.cashAction(), request.amount());
        accountRepository.save(account);
    }

    @Override
    @Transactional
    public void transferBalance(InternalTransferRequest request) {
        ApplicationUtil.validateAmount(request.amount());
        if (isRepeatedOperation(request.operationId(), KIND_TRANSFER,
                request.loginSender(), request.loginRecipient(), request.amount())) {
            return;
        }
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

    private boolean isRepeatedOperation(UUID operationId, String kind, String login,
                                        String counterparty, BigDecimal amount) {
        int inserted = processedOperationRepository.insertIfAbsent(operationId, kind, login, counterparty, amount);
        if (inserted == 1) {
            return false;
        }

        ProcessedOperation previous = processedOperationRepository.findById(operationId).orElseThrow();
        if (!previous.matches(kind, login, counterparty, amount)) {
            throw new IdempotencyConflictException(operationId);
        }
        log.info("Операция {} уже выполнена, повторное изменение баланса пропущено", operationId);
        return true;
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
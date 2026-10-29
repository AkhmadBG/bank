package ru.practicum.accounts.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.config.CurrentUserProvider;
import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.entity.OutboxEvent;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.InvalidAgeException;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.OutboxEventRepository;
import ru.practicum.accounts.service.AccountService;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationPublisher;
import ru.practicum.interaction.kafka.NotificationType;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import static ru.practicum.interaction.util.ApplicationConstant.MIN_AGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CurrentUserProvider currentUserProvider;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public ResultData getAccount() {
        String login = currentUserProvider.getCurrentLogin();
        Account account = findByLoginOrThrow(login);
        List<UserAccountDto> others = findOtherAccounts(login);
        return accountMapper.toResultData(account, others);
    }

    @Override
    @Transactional
    public ResultData editAccount(EditAccountRequest request) {
        String login = currentUserProvider.getCurrentLogin();
        Account account = findByLoginOrThrow(login);

        validateAge(request.birthdate());

        account.setName(request.name());
        account.setBirthdate(request.birthdate());
        accountRepository.save(account);

        saveAccountUpdateOutboxEvent(login);

        List<UserAccountDto> others = findOtherAccounts(login);
        return accountMapper.toResultData(account, others);
    }

    private void saveAccountUpdateOutboxEvent(String login) {
        UUID eventId = UUID.randomUUID();

        NotificationMessage message = new NotificationMessage(
                eventId,
                login,
                NotificationType.ACCOUNT_UPDATE,
                "Данные аккаунта изменены",
                null
        );

        OutboxEvent event = new OutboxEvent();
        event.setEventId(eventId);

        event.setOperationId(eventId);

        event.setEventType(NotificationType.ACCOUNT_UPDATE.name());
        event.setPayload(objectMapper.writeValueAsString(message));
        event.setCreatedAt(LocalDateTime.now());
        event.setAttempts(0);

        outboxEventRepository.save(event);
    }


    private Account findByLoginOrThrow(String login) {
        return accountRepository.findByLogin(login)
                .orElseThrow(() -> new AccountNotFoundException(login));
    }

    private List<UserAccountDto> findOtherAccounts(String excludeLogin) {
        return accountRepository.findAllByLoginNot(excludeLogin).stream()
                .map(acc -> new UserAccountDto(acc.getLogin(), acc.getName()))
                .toList();
    }

    private void validateAge(LocalDate birthdate) {
        if (birthdate == null || Period.between(birthdate, LocalDate.now()).getYears() < MIN_AGE) {
            throw new InvalidAgeException();
        }
    }

}
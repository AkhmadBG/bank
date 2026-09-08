package ru.practicum.accounts.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.InvalidAgeException;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.config.CurrentUserProvider;
import ru.practicum.accounts.service.AccountService;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.feignclient.NotificationFeignClient;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.notification.dto.NotificationRequest;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private static final int MIN_AGE = 18;

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationFeignClient notificationFeignClient;

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

        sendNotificationBestEffort(login, request);

        List<UserAccountDto> others = findOtherAccounts(login);
        return accountMapper.toResultData(account, others);
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

    private void sendNotificationBestEffort(String login, EditAccountRequest request) {
        try {
            String description = "Изменены данные аккаунта: имя — " + request.name()
                    + ", дата рождения — " + request.birthdate();
            notificationFeignClient.sendNotification(new NotificationRequest(login, description));
        } catch (Exception e) {
            log.warn("Не удалось отправить уведомление для {}: {}", login, e.getMessage());
        }
    }

}
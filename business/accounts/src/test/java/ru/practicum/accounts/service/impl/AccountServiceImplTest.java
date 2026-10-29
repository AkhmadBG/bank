package ru.practicum.accounts.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.accounts.config.CurrentUserProvider;
import ru.practicum.accounts.entity.Account;
import ru.practicum.accounts.entity.OutboxEvent;
import ru.practicum.accounts.exception.AccountNotFoundException;
import ru.practicum.accounts.exception.InvalidAgeException;
import ru.practicum.accounts.mapper.AccountMapper;
import ru.practicum.accounts.repository.AccountRepository;
import ru.practicum.accounts.repository.OutboxEventRepository;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.kafka.NotificationMessage;
import ru.practicum.interaction.kafka.NotificationType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static ru.practicum.interaction.util.ApplicationConstant.MIN_AGE;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    private static final String LOGIN = "test-user";
    private static final String OTHER_LOGIN = "other-user";
    private static final String ACCOUNT_UPDATE_MESSAGE = "Данные аккаунта изменены";

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private AccountServiceImpl accountService;

    private Account account;
    private ResultData resultData;

    @BeforeEach
    void setUp() {
        account = mock(Account.class);
        resultData = mock(ResultData.class);

        accountService = new AccountServiceImpl(
                accountRepository,
                accountMapper,
                currentUserProvider,
                outboxEventRepository,
                objectMapper
        );

        when(currentUserProvider.getCurrentLogin()).thenReturn(LOGIN);
    }

    @Test
    void shouldReturnCurrentAccountAndOtherUsers() {
        Account otherAccount = mock(Account.class);

        when(otherAccount.getLogin()).thenReturn(OTHER_LOGIN);
        when(otherAccount.getName()).thenReturn("Другой пользователь");

        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(account));

        when(accountRepository.findAllByLoginNot(LOGIN))
                .thenReturn(List.of(otherAccount));

        when(accountMapper.toResultData(eq(account), any()))
                .thenReturn(resultData);

        ResultData result = accountService.getAccount();

        assertSame(resultData, result);

        verify(currentUserProvider).getCurrentLogin();
        verify(accountRepository).findByLogin(LOGIN);
        verify(accountRepository).findAllByLoginNot(LOGIN);

        ArgumentCaptor<List<UserAccountDto>> captor =
                ArgumentCaptor.forClass(List.class);

        verify(accountMapper).toResultData(eq(account), captor.capture());

        List<UserAccountDto> others = captor.getValue();

        assertEquals(1, others.size());
        assertEquals(OTHER_LOGIN, others.get(0).login());
        assertEquals("Другой пользователь", others.get(0).name());

        verifyNoInteractions(outboxEventRepository);
    }

    @Test
    void shouldReturnEmptyListWhenThereAreNoOtherUsers() {
        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(account));

        when(accountRepository.findAllByLoginNot(LOGIN))
                .thenReturn(List.of());

        when(accountMapper.toResultData(account, List.of()))
                .thenReturn(resultData);

        ResultData result = accountService.getAccount();

        assertSame(resultData, result);

        verify(accountMapper).toResultData(account, List.of());
    }

    @Test
    void shouldThrowExceptionWhenCurrentAccountDoesNotExist() {
        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.getAccount()
        );

        verify(accountRepository, never()).findAllByLoginNot(any());
        verifyNoInteractions(accountMapper);
    }

    @Test
    void shouldUpdateAccountAndSaveOutboxEvent() throws Exception {
        LocalDate birthdate = LocalDate.now().minusYears(MIN_AGE + 5);
        EditAccountRequest request =
                new EditAccountRequest("Новое имя", birthdate);

        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(account));

        when(accountRepository.findAllByLoginNot(LOGIN))
                .thenReturn(List.of());

        when(accountMapper.toResultData(account, List.of()))
                .thenReturn(resultData);

        ResultData result = accountService.editAccount(request);

        assertSame(resultData, result);

        verify(account).setName("Новое имя");
        verify(account).setBirthdate(birthdate);
        verify(accountRepository).save(account);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);

        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent event = captor.getValue();

        assertNotNull(event.getEventId());
        assertEquals(event.getEventId(), event.getOperationId());
        assertEquals(
                NotificationType.ACCOUNT_UPDATE.name(),
                event.getEventType()
        );
        assertNotNull(event.getCreatedAt());
        assertEquals(0, event.getAttempts());
        assertNull(event.getPublishedAt());

        NotificationMessage message = objectMapper.readValue(
                event.getPayload(),
                NotificationMessage.class
        );

        assertEquals(event.getEventId(), message.eventId());
        assertEquals(LOGIN, message.login());
        assertEquals(NotificationType.ACCOUNT_UPDATE, message.type());
        assertEquals(ACCOUNT_UPDATE_MESSAGE, message.message());
        assertNull(message.amount());

        verify(accountRepository).findAllByLoginNot(LOGIN);
        verify(accountMapper).toResultData(account, List.of());
    }

    @Test
    void shouldAllowAccountWithExactlyMinimumAge() {
        LocalDate birthdate = LocalDate.now().minusYears(MIN_AGE);
        EditAccountRequest request =
                new EditAccountRequest("Новое имя", birthdate);

        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(account));

        when(accountRepository.findAllByLoginNot(LOGIN))
                .thenReturn(List.of());

        when(accountMapper.toResultData(account, List.of()))
                .thenReturn(resultData);

        assertDoesNotThrow(() -> accountService.editAccount(request));

        verify(accountRepository).save(account);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void shouldThrowExceptionWhenUserIsTooYoung() {
        LocalDate birthdate = LocalDate.now()
                .minusYears(MIN_AGE)
                .plusDays(1);

        EditAccountRequest request =
                new EditAccountRequest("Новое имя", birthdate);

        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidAgeException.class,
                () -> accountService.editAccount(request)
        );

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(outboxEventRepository);
        verify(accountMapper, never()).toResultData(any(), any());
    }

    @Test
    void shouldThrowExceptionWhenBirthdateIsNull() {
        EditAccountRequest request =
                new EditAccountRequest("Новое имя", null);

        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(account));

        assertThrows(
                InvalidAgeException.class,
                () -> accountService.editAccount(request)
        );

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(outboxEventRepository);
        verify(accountMapper, never()).toResultData(any(), any());
    }

    @Test
    void shouldThrowExceptionWhenAccountDoesNotExist() {
        EditAccountRequest request = new EditAccountRequest(
                "Новое имя",
                LocalDate.now().minusYears(MIN_AGE + 5)
        );

        when(accountRepository.findByLogin(LOGIN))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotFoundException.class,
                () -> accountService.editAccount(request)
        );

        verify(accountRepository, never()).save(any(Account.class));
        verifyNoInteractions(outboxEventRepository);
        verifyNoInteractions(accountMapper);
    }

}
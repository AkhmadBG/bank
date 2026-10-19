package ru.practicum.front.service.impl;

import feign.FeignException;
import feign.Request;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.practicum.front.exception.BankOperationException;
import ru.practicum.front.feignclient.GatewayAccountFeignClient;
import ru.practicum.front.feignclient.GatewayCashFeignClient;
import ru.practicum.front.feignclient.GatewayTransferFeignClient;
import ru.practicum.front.feignclient.NotificationsFeignClient;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.NotificationDto;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrontServiceImplTest {

    @Mock
    private GatewayAccountFeignClient accountFeignClient;
    @Mock
    private GatewayCashFeignClient cashFeignClient;
    @Mock
    private GatewayTransferFeignClient transferFeignClient;
    @Mock
    private NotificationsFeignClient notificationsFeignClient;

    private FrontServiceImpl frontService;

    private static final String NAME = "John Doe";
    private static final LocalDate BIRTHDATE = LocalDate.of(1990, 1, 1);
    private static final BigDecimal SUM = new BigDecimal("1000.00");
    private static final List<UserAccountDto> ACCOUNTS = List.of(
            new UserAccountDto("acc1", "name1"),
            new UserAccountDto("acc2", "name2"));
    private static final UUID OPERATION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        frontService = new FrontServiceImpl(
                accountFeignClient, cashFeignClient, transferFeignClient, notificationsFeignClient);
    }

    private ResultData stubAccount() {
        return new ResultData(NAME, BIRTHDATE, SUM, ACCOUNTS, null, null);
    }

    @Test
    void getAccount_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.getAccount();

        assertThat(result.name()).isEqualTo(NAME);
        assertThat(result.birthdate()).isEqualTo(BIRTHDATE);
        assertThat(result.sum()).isEqualByComparingTo(SUM);
        assertThat(result.accounts()).containsExactlyElementsOf(ACCOUNTS);
        assertThat(result.errors()).isNull();
        assertThat(result.info()).isNull();
        verify(accountFeignClient).getAccount();
    }

    @Test
    void getAccount_whenFetchFails_returnsFallback() {
        when(accountFeignClient.getAccount()).thenThrow(new RuntimeException("boom"));

        ResultData result = frontService.getAccount();

        assertThat(result.name()).isNull();
        assertThat(result.birthdate()).isNull();
        assertThat(result.sum()).isNull();
        assertThat(result.accounts()).isEmpty();
        assertThat(result.errors()).containsExactly("Не удалось загрузить данные аккаунта");
        assertThat(result.info()).isNull();
    }

    @Test
    void getAccount_whenResponseBodyIsNull_returnsFallback() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok().build());

        ResultData result = frontService.getAccount();

        assertThat(result.accounts()).isEmpty();
        assertThat(result.errors()).containsExactly("Не удалось загрузить данные аккаунта");
    }

    @Test
    void editAccount_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        ArgumentCaptor<EditAccountRequest> captor = ArgumentCaptor.forClass(EditAccountRequest.class);
        verify(accountFeignClient).editAccount(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo(NAME);
        assertThat(captor.getValue().birthdate()).isEqualTo(BIRTHDATE);

        assertThat(result.info()).isEqualTo("Данные сохранены");
        assertThat(result.errors()).isNull();
        assertThat(result.name()).isEqualTo(NAME);
    }

    @Test
    void editAccount_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("invalid data"))
                .when(accountFeignClient).editAccount(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors()).containsExactly("invalid data");
        assertThat(result.info()).isNull();
    }

    @Test
    void editAccount_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException()).when(accountFeignClient).editAccount(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors()).containsExactly("Сервис аккаунтов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editAccount_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class)).when(accountFeignClient).editAccount(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors()).containsExactly("Сервис аккаунтов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_put_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(OPERATION_ID, 100, CashAction.PUT);

        ArgumentCaptor<CashOperationRequest> captor = ArgumentCaptor.forClass(CashOperationRequest.class);
        verify(cashFeignClient).cashOperation(eq(OPERATION_ID), captor.capture());
        assertThat(captor.getValue().cashAction()).isEqualTo(CashAction.PUT);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(result.info()).isEqualTo("Счёт пополнен");
        assertThat(result.errors()).isNull();
    }

    @Test
    void editCash_get_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(OPERATION_ID, 50, CashAction.GET);

        ArgumentCaptor<CashOperationRequest> captor = ArgumentCaptor.forClass(CashOperationRequest.class);
        verify(cashFeignClient).cashOperation(eq(OPERATION_ID), captor.capture());
        assertThat(captor.getValue().cashAction()).isEqualTo(CashAction.GET);
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(50));

        assertThat(result.info()).isEqualTo("Средства сняты");
        assertThat(result.errors()).isNull();
    }

    @Test
    void editCash_shouldPassClientOperationIdUnchanged() {
        UUID key = UUID.randomUUID();
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        frontService.editCash(key, 100, CashAction.PUT);

        verify(cashFeignClient).cashOperation(eq(key), any(CashOperationRequest.class));
    }

    @Test
    void editCash_repeatedRequestWithSameKeySendsSameKey() {
        UUID key = UUID.randomUUID();
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        frontService.editCash(key, 100, CashAction.GET);
        frontService.editCash(key, 100, CashAction.GET);

        verify(cashFeignClient, times(2)).cashOperation(eq(key), any(CashOperationRequest.class));
    }

    @Test
    void editCash_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("insufficient funds"))
                .when(cashFeignClient).cashOperation(eq(OPERATION_ID), any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(OPERATION_ID, 100, CashAction.GET);

        assertThat(result.errors()).containsExactly("insufficient funds");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException()).when(cashFeignClient).cashOperation(eq(OPERATION_ID), any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(OPERATION_ID, 100, CashAction.PUT);

        assertThat(result.errors()).containsExactly("Сервис обналичивания временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(cashFeignClient).cashOperation(eq(OPERATION_ID), any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(OPERATION_ID, 100, CashAction.PUT);

        assertThat(result.errors()).containsExactly("Сервис обналичивания временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(OPERATION_ID, 200, "recipient");

        ArgumentCaptor<TransferOperationRequest> captor =
                ArgumentCaptor.forClass(TransferOperationRequest.class);
        verify(transferFeignClient).transferOperation(eq(OPERATION_ID), captor.capture());
        assertThat(captor.getValue().loginRecipient()).isEqualTo("recipient");
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(200));

        assertThat(result.info()).isEqualTo("Перевод выполнен");
        assertThat(result.errors()).isNull();
    }

    @Test
    void transfer_shouldPassClientOperationIdUnchanged() {
        UUID key = UUID.randomUUID();
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        frontService.transfer(key, 200, "recipient");

        verify(transferFeignClient).transferOperation(eq(key), any(TransferOperationRequest.class));
    }

    @Test
    void transfer_repeatedRequestWithSameKeySendsSameKey() {
        UUID key = UUID.randomUUID();
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        frontService.transfer(key, 200, "recipient");
        frontService.transfer(key, 200, "recipient");

        verify(transferFeignClient, times(2)).transferOperation(eq(key), any(TransferOperationRequest.class));
    }

    @Test
    void transfer_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("recipient not found"))
                .when(transferFeignClient).transferOperation(eq(OPERATION_ID), any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.errors()).containsExactly("recipient not found");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException()).when(transferFeignClient).transferOperation(eq(OPERATION_ID), any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.errors()).containsExactly("Сервис переводов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(transferFeignClient).transferOperation(eq(OPERATION_ID), any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.errors()).containsExactly("Сервис переводов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_whenStateRefreshFailsAfterOperation_returnsFallbackWithError() {
        when(accountFeignClient.getAccount()).thenThrow(new RuntimeException("gateway down"));

        ResultData result = frontService.transfer(OPERATION_ID, 200, "recipient");

        verify(transferFeignClient).transferOperation(eq(OPERATION_ID), any(TransferOperationRequest.class));
        assertThat(result.accounts()).isEmpty();
        assertThat(result.errors()).containsExactly("Не удалось загрузить данные аккаунта");
    }

    @Test
    void getNotifications_success() {
        List<NotificationDto> notifications = List.of(mock(NotificationDto.class), mock(NotificationDto.class));
        when(notificationsFeignClient.getNotifications("petrov")).thenReturn(notifications);

        List<NotificationDto> result = frontService.getNotifications("petrov");

        assertThat(result).isSameAs(notifications);
        verify(notificationsFeignClient).getNotifications("petrov");
        verifyNoInteractions(accountFeignClient, cashFeignClient, transferFeignClient);
    }

    @Test
    void getNotifications_whenNoNotifications_returnsEmptyList() {
        when(notificationsFeignClient.getNotifications("petrov")).thenReturn(List.of());

        assertThat(frontService.getNotifications("petrov")).isEmpty();
    }

    @Test
    void getNotifications_whenServiceFails_returnsEmptyListInsteadOfError() {
        when(notificationsFeignClient.getNotifications("petrov")).thenThrow(buildFeignException());

        List<NotificationDto> result = frontService.getNotifications("petrov");

        assertThat(result).isEmpty();
    }

    private FeignException buildFeignException() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/test",
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8,
                null);
        return FeignException.errorStatus("test", feign.Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(Map.of())
                .build());
    }

}
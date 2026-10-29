
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
import ru.practicum.front.exception.BankOperationException;
import ru.practicum.front.feignclient.NotificationsFeignClient;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.NotificationDto;
import ru.practicum.interaction.front.dto.ResultData;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrontServiceImplTest {

    @Mock
    private FrontGatewayService frontGatewayService;

    @Mock
    private NotificationsFeignClient notificationsFeignClient;

    private FrontServiceImpl frontService;

    private static final String NAME = "John Doe";
    private static final LocalDate BIRTHDATE = LocalDate.of(1990, 1, 1);
    private static final BigDecimal SUM = new BigDecimal("1000.00");

    private static final List<UserAccountDto> ACCOUNTS = List.of(
            new UserAccountDto("acc1", "name1"),
            new UserAccountDto("acc2", "name2")
    );

    private static final UUID OPERATION_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        frontService = new FrontServiceImpl(
                frontGatewayService,
                notificationsFeignClient
        );
    }

    private ResultData stubAccount() {
        return new ResultData(NAME, BIRTHDATE, SUM, ACCOUNTS, null, null);
    }

    private void stubCurrentAccount() {
        when(frontGatewayService.fetchAccount()).thenReturn(stubAccount());
    }

    @Test
    void getAccount_success() {
        stubCurrentAccount();

        ResultData result = frontService.getAccount();

        assertThat(result.name()).isEqualTo(NAME);
        assertThat(result.birthdate()).isEqualTo(BIRTHDATE);
        assertThat(result.sum()).isEqualByComparingTo(SUM);
        assertThat(result.accounts()).containsExactlyElementsOf(ACCOUNTS);
        assertThat(result.errors()).isNull();
        assertThat(result.info()).isNull();

        verify(frontGatewayService).fetchAccount();
    }

    @Test
    void getAccount_whenFetchFails_returnsFallback() {
        when(frontGatewayService.fetchAccount())
                .thenThrow(new RuntimeException("boom"));

        ResultData result = frontService.getAccount();

        assertThat(result.name()).isNull();
        assertThat(result.birthdate()).isNull();
        assertThat(result.sum()).isNull();
        assertThat(result.accounts()).isEmpty();
        assertThat(result.errors())
                .containsExactly("Не удалось загрузить данные аккаунта");
        assertThat(result.info()).isNull();
    }

    @Test
    void getAccount_whenResponseBodyIsNull_returnsFallback() {
        when(frontGatewayService.fetchAccount()).thenReturn(null);

        ResultData result = frontService.getAccount();

        assertThat(result.accounts()).isEmpty();
        assertThat(result.errors())
                .containsExactly("Не удалось загрузить данные аккаунта");
    }

    @Test
    void editAccount_success() {
        stubCurrentAccount();

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        verify(frontGatewayService).editAccount(NAME, BIRTHDATE);

        assertThat(result.info()).isEqualTo("Данные сохранены");
        assertThat(result.errors()).isNull();
        assertThat(result.name()).isEqualTo(NAME);
    }

    @Test
    void editAccount_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("invalid data"))
                .when(frontGatewayService).editAccount(NAME, BIRTHDATE);
        stubCurrentAccount();

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors()).containsExactly("invalid data");
        assertThat(result.info()).isNull();
    }

    @Test
    void editAccount_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException())
                .when(frontGatewayService).editAccount(NAME, BIRTHDATE);
        stubCurrentAccount();

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors())
                .containsExactly("Сервис аккаунтов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editAccount_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(frontGatewayService).editAccount(NAME, BIRTHDATE);
        stubCurrentAccount();

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors())
                .containsExactly("Сервис аккаунтов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_put_success() {
        stubCurrentAccount();

        ResultData result =
                frontService.editCash(OPERATION_ID, 100, CashAction.PUT);

        verify(frontGatewayService)
                .cashOperation(OPERATION_ID, CashAction.PUT, 100);

        assertThat(result.info()).isEqualTo("Счёт пополнен");
        assertThat(result.errors()).isNull();
    }

    @Test
    void editCash_get_success() {
        stubCurrentAccount();

        ResultData result =
                frontService.editCash(OPERATION_ID, 50, CashAction.GET);

        verify(frontGatewayService)
                .cashOperation(OPERATION_ID, CashAction.GET, 50);

        assertThat(result.info()).isEqualTo("Средства сняты");
        assertThat(result.errors()).isNull();
    }

    @Test
    void editCash_shouldPassClientOperationIdUnchanged() {
        UUID key = UUID.randomUUID();
        stubCurrentAccount();

        frontService.editCash(key, 100, CashAction.PUT);

        verify(frontGatewayService)
                .cashOperation(key, CashAction.PUT, 100);
    }

    @Test
    void editCash_repeatedRequestWithSameKeySendsSameKey() {
        UUID key = UUID.randomUUID();
        stubCurrentAccount();

        frontService.editCash(key, 100, CashAction.GET);
        frontService.editCash(key, 100, CashAction.GET);

        verify(frontGatewayService, times(2))
                .cashOperation(key, CashAction.GET, 100);
    }

    @Test
    void editCash_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("insufficient funds"))
                .when(frontGatewayService)
                .cashOperation(OPERATION_ID, CashAction.GET, 100);
        stubCurrentAccount();

        ResultData result =
                frontService.editCash(OPERATION_ID, 100, CashAction.GET);

        assertThat(result.errors()).containsExactly("insufficient funds");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException())
                .when(frontGatewayService)
                .cashOperation(OPERATION_ID, CashAction.PUT, 100);
        stubCurrentAccount();

        ResultData result =
                frontService.editCash(OPERATION_ID, 100, CashAction.PUT);

        assertThat(result.errors())
                .containsExactly("Сервис для работы с наличными временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(frontGatewayService)
                .cashOperation(OPERATION_ID, CashAction.PUT, 100);
        stubCurrentAccount();

        ResultData result =
                frontService.editCash(OPERATION_ID, 100, CashAction.PUT);

        assertThat(result.errors())
                .containsExactly("Сервис обналичивания временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_success() {
        stubCurrentAccount();

        ResultData result =
                frontService.transfer(OPERATION_ID, 200, "recipient");

        verify(frontGatewayService)
                .transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.info()).isEqualTo("Перевод выполнен");
        assertThat(result.errors()).isNull();
    }

    @Test
    void transfer_shouldPassClientOperationIdUnchanged() {
        UUID key = UUID.randomUUID();
        stubCurrentAccount();

        frontService.transfer(key, 200, "recipient");

        verify(frontGatewayService).transfer(key, 200, "recipient");
    }

    @Test
    void transfer_repeatedRequestWithSameKeySendsSameKey() {
        UUID key = UUID.randomUUID();
        stubCurrentAccount();

        frontService.transfer(key, 200, "recipient");
        frontService.transfer(key, 200, "recipient");

        verify(frontGatewayService, times(2))
                .transfer(key, 200, "recipient");
    }

    @Test
    void transfer_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("recipient not found"))
                .when(frontGatewayService)
                .transfer(OPERATION_ID, 200, "recipient");
        stubCurrentAccount();

        ResultData result =
                frontService.transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.errors())
                .containsExactly("recipient not found");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException())
                .when(frontGatewayService)
                .transfer(OPERATION_ID, 200, "recipient");
        stubCurrentAccount();

        ResultData result =
                frontService.transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.errors())
                .containsExactly("Сервис переводов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(frontGatewayService)
                .transfer(OPERATION_ID, 200, "recipient");
        stubCurrentAccount();

        ResultData result =
                frontService.transfer(OPERATION_ID, 200, "recipient");

        assertThat(result.errors())
                .containsExactly("Сервис переводов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_whenStateRefreshFailsAfterOperation_returnsFallbackWithError() {
        doThrow(new RuntimeException("gateway down"))
                .when(frontGatewayService).fetchAccount();

        ResultData result =
                frontService.transfer(OPERATION_ID, 200, "recipient");

        verify(frontGatewayService)
                .transfer(OPERATION_ID, 200, "recipient");
        assertThat(result.accounts()).isEmpty();
        assertThat(result.errors())
                .containsExactly("Не удалось загрузить данные аккаунта");
    }

    @Test
    void getNotifications_success() {
        List<NotificationDto> notifications = List.of(
                mock(NotificationDto.class),
                mock(NotificationDto.class)
        );
        when(notificationsFeignClient.getNotifications("petrov"))
                .thenReturn(notifications);

        List<NotificationDto> result =
                frontService.getNotifications("petrov");

        assertThat(result).isSameAs(notifications);
        verify(notificationsFeignClient).getNotifications("petrov");
        verifyNoInteractions(frontGatewayService);
    }

    @Test
    void getNotifications_whenNoNotifications_returnsEmptyList() {
        when(notificationsFeignClient.getNotifications("petrov"))
                .thenReturn(List.of());

        assertThat(frontService.getNotifications("petrov")).isEmpty();
    }

    @Test
    void getNotifications_whenServiceFails_returnsEmptyListInsteadOfError() {
        when(notificationsFeignClient.getNotifications("petrov"))
                .thenThrow(buildFeignException());

        List<NotificationDto> result =
                frontService.getNotifications("petrov");

        assertThat(result).isEmpty();
    }

    private FeignException buildFeignException() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/test",
                Map.of(),
                new byte[0],
                StandardCharsets.UTF_8,
                null
        );

        return FeignException.errorStatus(
                "test",
                feign.Response.builder()
                        .status(503)
                        .reason("Service Unavailable")
                        .request(request)
                        .headers(Map.of())
                        .build()
        );
    }

}
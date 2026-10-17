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
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.cash.dto.CashOperationRequest;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.ResultData;
import ru.practicum.interaction.transfer.dto.TransferOperationRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrontServiceImplTest {

    @Mock
    private GatewayAccountFeignClient accountFeignClient;
    @Mock
    private GatewayCashFeignClient cashFeignClient;
    @Mock
    private GatewayTransferFeignClient transferFeignClient;

    private FrontServiceImpl frontService;

    private static final String NAME = "John Doe";
    private static final LocalDate BIRTHDATE = LocalDate.of(1990, 1, 1);
    private static final BigDecimal SUM = new BigDecimal("1000.00");
    private static final List<UserAccountDto> ACCOUNTS = List.of(new UserAccountDto("acc1", "name1"),
            new UserAccountDto("acc2", "name2"));

    @BeforeEach
    void setUp() {
        frontService = new FrontServiceImpl(accountFeignClient, cashFeignClient, transferFeignClient);
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
        FeignException feignException = buildFeignException();
        doThrow(feignException).when(accountFeignClient).editAccount(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors()).containsExactly("Сервис аккаунтов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editAccount_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(accountFeignClient).editAccount(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editAccount(NAME, BIRTHDATE);

        assertThat(result.errors()).containsExactly("Сервис аккаунтов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_put_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(100, CashAction.PUT);

        ArgumentCaptor<CashOperationRequest> captor = ArgumentCaptor.forClass(CashOperationRequest.class);
        verify(cashFeignClient).cashOperation(captor.capture());
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));

        assertThat(result.info()).isEqualTo("Счёт пополнен");
        assertThat(result.errors()).isNull();
    }

    @Test
    void editCash_get_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(50, CashAction.GET);

        assertThat(result.info()).isEqualTo("Средства сняты");
        assertThat(result.errors()).isNull();
    }

    @Test
    void editCash_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("insufficient funds"))
                .when(cashFeignClient).cashOperation(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(100, CashAction.GET);

        assertThat(result.errors()).containsExactly("insufficient funds");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException()).when(cashFeignClient).cashOperation(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(100, CashAction.PUT);

        assertThat(result.errors()).containsExactly("Сервис обналичивания временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void editCash_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class)).when(cashFeignClient).cashOperation(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.editCash(100, CashAction.PUT);

        assertThat(result.errors()).containsExactly("Сервис обналичивания временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_success() {
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(200, "recipient");

        ArgumentCaptor<TransferOperationRequest> captor =
                ArgumentCaptor.forClass(TransferOperationRequest.class);
        verify(transferFeignClient).transferOperation(captor.capture());
        assertThat(captor.getValue().loginRecipient()).isEqualTo("recipient");
        assertThat(captor.getValue().amount()).isEqualByComparingTo(BigDecimal.valueOf(200));

        assertThat(result.info()).isEqualTo("Перевод выполнен");
        assertThat(result.errors()).isNull();
    }

    @Test
    void transfer_bankOperationException_returnsErrorMessage() {
        doThrow(new BankOperationException("recipient not found"))
                .when(transferFeignClient).transferOperation(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(200, "recipient");

        assertThat(result.errors()).containsExactly("recipient not found");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_feignException_returnsUnavailableMessage() {
        doThrow(buildFeignException()).when(transferFeignClient).transferOperation(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(200, "recipient");

        assertThat(result.errors()).containsExactly("Сервис переводов временно недоступен");
        assertThat(result.info()).isNull();
    }

    @Test
    void transfer_callNotPermitted_returnsUnavailableMessage() {
        doThrow(mock(CallNotPermittedException.class))
                .when(transferFeignClient).transferOperation(any());
        when(accountFeignClient.getAccount()).thenReturn(ResponseEntity.ok(stubAccount()));

        ResultData result = frontService.transfer(200, "recipient");

        assertThat(result.errors()).containsExactly("Сервис переводов временно недоступен");
        assertThat(result.info()).isNull();
    }

    private FeignException buildFeignException() {
        Request request = Request.create(
                Request.HttpMethod.POST,
                "/test",
                java.util.Map.of(),
                new byte[0],
                StandardCharsets.UTF_8,
                null);
        return FeignException.errorStatus("test", feign.Response.builder()
                .status(503)
                .reason("Service Unavailable")
                .request(request)
                .headers(java.util.Map.of())
                .build());
    }

}
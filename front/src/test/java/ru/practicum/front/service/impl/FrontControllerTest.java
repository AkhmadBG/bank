package ru.practicum.front.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import ru.practicum.front.controller.FrontController;
import ru.practicum.front.service.FrontService;
import ru.practicum.interaction.front.dto.NotificationDto;
import ru.practicum.interaction.front.dto.ResultData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FrontControllerTest {

    private FrontService frontService;
    private FrontController frontController;

    @BeforeEach
    void setUp() {
        frontService = mock(FrontService.class);
        frontController = new FrontController(frontService);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "test-user",
                        "password",
                        List.of()
                )
        );

        when(frontService.getNotifications("test-user"))
                .thenReturn(List.<NotificationDto>of());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transferShouldPreserveOperationIdWhenResultIsUncertain() {
        UUID originalOperationId = UUID.randomUUID();

        ResultData errorResult = new ResultData(
                "Test User",
                LocalDate.of(1990, 1, 1),
                new BigDecimal("1000.00"),
                List.of(),
                List.of("Сервис переводов временно недоступен"),
                null
        );

        when(frontService.transfer(originalOperationId, 200, "recipient"))
                .thenReturn(errorResult);

        Model model = new ExtendedModelMap();

        String view = frontController.transfer(
                model,
                200,
                "recipient",
                originalOperationId
        );

        assertThat(view).isEqualTo("main");

        assertThat(model.getAttribute("transferOperationId"))
                .isEqualTo(originalOperationId);

        verify(frontService).transfer(
                originalOperationId,
                200,
                "recipient"
        );
    }

    @Test
    void transferShouldGenerateNewOperationIdAfterConfirmedSuccess() {
        UUID originalOperationId = UUID.randomUUID();

        ResultData successResult = new ResultData(
                "Test User",
                LocalDate.of(1990, 1, 1),
                new BigDecimal("800.00"),
                List.of(),
                null,
                "Перевод выполнен"
        );

        when(frontService.transfer(originalOperationId, 200, "recipient"))
                .thenReturn(successResult);

        Model model = new ExtendedModelMap();

        String view = frontController.transfer(
                model,
                200,
                "recipient",
                originalOperationId
        );

        assertThat(view).isEqualTo("main");

        Object attribute = model.getAttribute("transferOperationId");

        assertThat(attribute).isInstanceOf(UUID.class);
        assertThat((UUID) attribute).isNotEqualTo(originalOperationId);

        verify(frontService).transfer(
                originalOperationId,
                200,
                "recipient"
        );
    }

}
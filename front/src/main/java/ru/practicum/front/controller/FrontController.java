package ru.practicum.front.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.front.service.FrontService;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.NotificationDto;
import ru.practicum.interaction.front.dto.ResultData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FrontController {

    private final FrontService frontService;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(Model model) {
        ResultData result = frontService.getAccount();
        model.addAttribute("result", result);
        model.addAttribute("cashOperationId", UUID.randomUUID());
        model.addAttribute("transferOperationId", UUID.randomUUID());
        addNotifications(model);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(
            Model model,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate
    ) {
        ResultData result = frontService.editAccount(name, birthdate);
        model.addAttribute("result", result);
        model.addAttribute("cashOperationId", UUID.randomUUID());
        model.addAttribute("transferOperationId", UUID.randomUUID());
        addNotifications(model);
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action,
            @RequestParam("operationId") UUID operationId
    ) {
        ResultData result = frontService.editCash(operationId, value, action);
        model.addAttribute("result", result);
        model.addAttribute("cashOperationId",
                result.info() != null ? UUID.randomUUID() : operationId);
        model.addAttribute("transferOperationId", UUID.randomUUID());
        addNotifications(model);
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login,
            @RequestParam("operationId") UUID operationId
    ) {
        ResultData result = frontService.transfer(operationId, value, login);
        model.addAttribute("result", result);

        model.addAttribute("transferOperationId",
                result.info() != null ? UUID.randomUUID() : operationId);
        model.addAttribute("cashOperationId", UUID.randomUUID());
        addNotifications(model);
        return "main";
    }

    private void addNotifications(Model model) {
        String login = getCurrentLogin();
        List<NotificationDto> notifications =
                frontService.getNotifications(login);

        model.addAttribute("notifications", notifications);
    }

    private String getCurrentLogin() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2AuthenticationToken oauth2) {
            String username = oauth2.getPrincipal()
                    .getAttribute("preferred_username");

            if (username != null && !username.isBlank()) {
                return username;
            }
        }

        return authentication.getName();
    }

}
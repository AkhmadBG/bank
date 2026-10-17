package ru.practicum.front.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.front.service.FrontService;
import ru.practicum.interaction.cash.enums.CashAction;
import ru.practicum.interaction.front.dto.ResultData;

import java.time.LocalDate;

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
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action
    ) {
        ResultData result = frontService.editCash(value, action);
        model.addAttribute("result", result);
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(
            Model model,
            @RequestParam("value") int value,
            @RequestParam("login") String login
    ) {
        ResultData result = frontService.transfer(value, login);
        model.addAttribute("result", result);
        return "main";
    }

}
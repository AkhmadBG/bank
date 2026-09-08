package ru.practicum.interaction.apiinterface;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.interaction.account.dto.EditAccountRequest;
import ru.practicum.interaction.front.dto.ResultData;

public interface AccountOperations {

    @GetMapping
    ResponseEntity<ResultData> getAccount();

    @PatchMapping
    ResponseEntity<ResultData> editAccount(@RequestBody EditAccountRequest editAccountRequest);

}
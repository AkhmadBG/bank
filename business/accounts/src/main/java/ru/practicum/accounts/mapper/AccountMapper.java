package ru.practicum.accounts.mapper;

import org.mapstruct.Mapper;
import ru.practicum.accounts.entity.Account;
import ru.practicum.interaction.account.dto.UserAccountDto;
import ru.practicum.interaction.front.dto.ResultData;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    default ResultData toResultData(Account account, List<UserAccountDto> otherAccounts) {
        return new ResultData(
                account.getName(),
                account.getBirthdate(),
                account.getBalance(),
                otherAccounts,
                null,
                null
        );
    }

}
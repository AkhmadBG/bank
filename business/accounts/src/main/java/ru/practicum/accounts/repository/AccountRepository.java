package ru.practicum.accounts.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.accounts.entity.Account;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByLogin(String login);

    List<Account> findAllByLoginNot(String login);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.login = :login")
    Optional<Account> findByLoginForUpdate(String login);

}
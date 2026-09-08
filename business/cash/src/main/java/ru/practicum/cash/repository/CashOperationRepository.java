package ru.practicum.cash.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.cash.entity.CashOperation;

@Repository
public interface CashOperationRepository extends JpaRepository<CashOperation, Long> {
}

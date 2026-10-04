package ru.practicum.transfer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.transfer.entity.TransferOperation;

@Repository
public interface TransferOperationRepository extends JpaRepository<TransferOperation, Long> {
}
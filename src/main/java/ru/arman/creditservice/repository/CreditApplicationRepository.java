package ru.arman.creditservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arman.creditservice.domain.entity.CreditApplicationEntity;

import java.util.UUID;

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplicationEntity, UUID> {
}

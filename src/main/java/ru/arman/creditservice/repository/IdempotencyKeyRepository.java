package ru.arman.creditservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.arman.creditservice.domain.entity.IdempotencyKeyEntity;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKeyEntity, String> {
}

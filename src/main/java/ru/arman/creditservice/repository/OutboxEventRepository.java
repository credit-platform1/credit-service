package ru.arman.creditservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.arman.creditservice.domain.entity.OutboxEventEntity;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAsc(
            OutboxEventEntity.OutboxStatus status
    );

    @Query(value = """
        SELECT * FROM outbox_events 
        WHERE status = 'PENDING' 
        ORDER BY created_at ASC 
        LIMIT 100 
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List <OutboxEventEntity> findTop100ByStatusOrderByCreatedAtAscNative();
}

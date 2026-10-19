package ru.arman.creditservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.arman.creditservice.domain.entity.ApplicationStatusHistoryEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApplicationStatusHistoryRepository extends
        JpaRepository<ApplicationStatusHistoryEntity, UUID> {
    List<ApplicationStatusHistoryEntity> findByApplicationIdOrderByChangedAtAsc(
            UUID applicationId
    );

    @Modifying
    @Query("""
            DELETE FROM ApplicationStatusHistoryEntity h
            WHERE h.applicationId = :applicationId
            """)
    int deleteByApplicationId(@Param("applicationId") UUID applicationId);
}

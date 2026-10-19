package ru.arman.creditservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.arman.creditservice.domain.model.ApplicationStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ApplicationStatus toStatus;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;
}

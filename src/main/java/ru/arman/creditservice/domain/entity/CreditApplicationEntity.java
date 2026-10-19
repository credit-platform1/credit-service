package ru.arman.creditservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import ru.arman.creditservice.domain.model.ApplicationStatus;
import ru.arman.creditservice.domain.model.EmploymentType;
import ru.arman.creditservice.domain.model.LoanPurpose;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credit_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditApplicationEntity {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "customer_first_name", nullable = false, length = 100)
    private String customerFirstName;

    @Column(name = "customer_last_name", nullable = false, length = 100)
    private String customerLastName;

    @Column(name = "customer_middle_name", length = 100)
    private String customerMiddleName;

    @Column(name = "customer_birth_date", nullable = false)
    private LocalDate customerBirthDate;

    @Column(name = "customer_inn", nullable = false, length = 12)
    private String customerInn;

    @Column(name = "customer_phone", nullable = false, length = 20)
    private String customerPhone;

    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Column(name = "requested_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 30)
    private LoanPurpose purpose;

    @Column(name = "monthly_income", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyIncome;

    @Column(name = "monthly_expenses", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyExpenses;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 30)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ApplicationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

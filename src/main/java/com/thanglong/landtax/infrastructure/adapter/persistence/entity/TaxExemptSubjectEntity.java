package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_exempt_subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxExemptSubjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exempt_id")
    private Integer exemptId;

    @Column(name = "citizen_id", nullable = false)
    private Integer citizenId;

    @Column(name = "uploaded_by_account")
    private Integer uploadedByAccount;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "exemption_reason", columnDefinition = "TEXT")
    private String exemptionReason;

    @Column(name = "discount_rate", precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "applied_year")
    private Integer appliedYear;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}

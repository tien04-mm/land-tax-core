package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA Entity mapping với bảng tax_exempt_subjects trong CSDL.
 */
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

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "exemption_reason", columnDefinition = "TEXT")
    private String exemptionReason;

    @Column(name = "discount_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountRate;

    @Column(name = "applied_year", nullable = false)
    private Integer appliedYear;

    @Column(name = "uploaded_by_account", nullable = false)
    private Integer uploadedByAccount;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}

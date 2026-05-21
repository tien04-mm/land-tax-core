package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tax_declarations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "declaration_id")
    private Integer declarationId;

    @Column(name = "record_id")
    private Integer recordId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", insertable = false, updatable = false)
    private RecordEntity record;

    @Column(name = "declared_area", precision = 10, scale = 2)
    private BigDecimal declaredArea;

    @Column(name = "declared_usage", length = 100)
    private String declaredUsage;

    @Column(name = "declaration_notes", columnDefinition = "TEXT")
    private String declarationNotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

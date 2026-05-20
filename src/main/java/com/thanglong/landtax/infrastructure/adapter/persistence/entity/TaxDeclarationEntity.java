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

    @Column(name = "citizen_id")
    private Integer citizenId;

    @Column(name = "sender_cccd", length = 20)
    private String senderCccd;

    @Column(name = "parcel_id")
    private Integer parcelId;

    @Column(name = "tax_year")
    private Integer taxYear;

    @Column(name = "declared_area", precision = 10, scale = 2)
    private BigDecimal declaredArea;

    @Column(name = "actual_area", precision = 10, scale = 2)
    private BigDecimal actualArea;

    @Column(name = "declared_purpose", length = 255)
    private String declaredPurpose;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "calculated_tax_amount", precision = 15, scale = 2)
    private BigDecimal calculatedTaxAmount;

    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "supporting_documents", columnDefinition = "TEXT")
    private String supportingDocuments; // Comma-separated URLs/links (so do photo, giay to chuyen nhuong)

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }
}

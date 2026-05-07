package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping chính xác với bảng records trong land_tax_management.sql.
 * Bảng records chứa tờ khai/hồ sơ đất đai (bao gồm tờ khai thuế).
 *
 * <p>record_category dùng để phân loại: TAX_DECLARATION, TRANSFER, CHANGE_PURPOSE, ...</p>
 * <p>current_status: PENDING, APPROVED, REJECTED, WARNING_FRAUD, ...</p>
 */
@Entity
@Table(name = "records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @Column(name = "citizen_id", nullable = false)
    private Integer citizenId;                  // FK → citizens.citizen_id

    @Column(name = "land_parcel_id", nullable = false)
    private Integer landParcelId;               // FK → land_parcels.land_parcel_id

    @Column(name = "record_category", nullable = false, length = 50)
    private String recordCategory;              // TAX_DECLARATION, TRANSFER, ...

    @Column(name = "current_status", length = 50)
    private String currentStatus;               // PENDING, APPROVED, WARNING_FRAUD, ...

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (currentStatus == null) {
            currentStatus = "PENDING";
        }
    }
}

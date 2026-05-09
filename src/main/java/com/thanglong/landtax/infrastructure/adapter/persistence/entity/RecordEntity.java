package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * JPA Entity mapping chinh xac voi bang records trong land_tax_management.sql.
 * Bang records chua to khai/ho so dat dai (bao gom to khai thue).
 *
 * <p>record_category dung de phan loai: TAX_DECLARATION, TRANSFER, CHANGE_PURPOSE, ...</p>
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
    private Integer citizenId;                  // FK  citizens.citizen_id

    @Column(name = "land_parcel_id", nullable = false)
    private Integer landParcelId;               // FK  land_parcels.land_parcel_id

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

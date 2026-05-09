package com.thanglong.landtax.infrastructure.adapter.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "land_owner_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandOwnerHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long historyId;

    @Column(name = "parcel_id", nullable = false)
    private Integer parcelId;

    @Column(name = "old_owner_cccd", nullable = false, length = 12)
    private String oldOwnerCccd;

    @Column(name = "new_owner_cccd", nullable = false, length = 12)
    private String newOwnerCccd;

    @Column(name = "mutation_type", length = 30)
    private String mutationType;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (effectiveDate == null) {
            effectiveDate = LocalDate.now();
        }
    }
}

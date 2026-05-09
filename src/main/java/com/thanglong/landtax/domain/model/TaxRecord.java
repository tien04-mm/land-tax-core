package com.thanglong.landtax.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain model dai dien cho ban ghi thue dat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRecord {

    private Long id;
    private Long landParcelId;
    private Long citizenId;
    private Integer taxYear;
    private BigDecimal taxableArea;      // Dien tich chiu thue
    private BigDecimal taxRate;          // Thue suat (%)
    private BigDecimal taxAmount;        // So tien thue
    private String status;              // PENDING, APPROVED, PAID, OVERDUE
    private String notes;
    private LocalDateTime declarationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxExemptSubjectResponseDTO {
    private Integer exemptId;
    private Integer citizenId;
    private Integer uploadedByAccount;
    private String fullName;
    private String exemptionReason;
    private BigDecimal discountRate;
    private Integer appliedYear;
    private LocalDateTime uploadedAt;
    private String status;
}

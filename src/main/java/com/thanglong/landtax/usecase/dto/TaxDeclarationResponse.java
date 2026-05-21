package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phan hoi sau khi nop to khai thue dat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationResponse {

    private Integer recordId;
    private Integer citizenId;
    private Integer parcelId;
    private BigDecimal declaredArea;
    private String declaredUsage;
    private String status;                      // mapped from RecordEntity
    private String declarationNotes;
    private LocalDateTime createdAt;
}

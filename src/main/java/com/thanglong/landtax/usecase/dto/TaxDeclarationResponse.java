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
    private Integer taxYear;
    private BigDecimal declaredArea;
    private BigDecimal actualArea;
    private String declaredPurpose;
    private String status;                      // PENDING, WARNING_FRAUD
    private String reviewNote;                  // Ghi chu canh bao gian lan
    private BigDecimal calculatedTaxAmount;     // So tien thue tinh duoc
    private BigDecimal unitPrice;               // Don gia dat ap dung
    private BigDecimal taxRate;                 // Thue suat ap dung
    private LocalDateTime submittedAt;
}

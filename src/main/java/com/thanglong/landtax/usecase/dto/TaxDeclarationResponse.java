package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phản hồi sau khi nộp tờ khai thuế đất.
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
    private String reviewNote;                  // Ghi chú cảnh báo gian lận
    private BigDecimal calculatedTaxAmount;     // Số tiền thuế tính được
    private BigDecimal unitPrice;               // Đơn giá đất áp dụng
    private BigDecimal taxRate;                 // Thuế suất áp dụng
    private LocalDateTime submittedAt;
}

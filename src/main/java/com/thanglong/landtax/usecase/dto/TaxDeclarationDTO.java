package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO cho to khai thue dat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationDTO {

    private Long id;

    @NotNull(message = "Ma thua dat khong duoc de trong")
    private Long landParcelId;

    @NotNull(message = "ID cong dan khong duoc de trong")
    private Long citizenId;

    @NotNull(message = "Nam tinh thue khong duoc de trong")
    private Integer taxYear;

    @NotNull(message = "Dien tich chiu thue khong duoc de trong")
    private BigDecimal taxableArea;

    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private String status;
    private String notes;
}

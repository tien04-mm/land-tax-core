package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO cho tờ khai thuế đất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationDTO {

    private Long id;

    @NotNull(message = "Mã thửa đất không được để trống")
    private Long landParcelId;

    @NotNull(message = "ID công dân không được để trống")
    private Long citizenId;

    @NotNull(message = "Năm tính thuế không được để trống")
    private Integer taxYear;

    @NotNull(message = "Diện tích chịu thuế không được để trống")
    private BigDecimal taxableArea;

    private BigDecimal taxRate;
    private BigDecimal taxAmount;
    private String status;
    private String notes;
}

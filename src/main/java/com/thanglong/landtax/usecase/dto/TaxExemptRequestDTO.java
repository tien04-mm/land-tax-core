package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxExemptRequestDTO {
    @NotNull(message = "exemptionReason không được để trống")
    private String exemptionReason;

    @NotNull(message = "discountRate không được để trống")
    @Positive(message = "discountRate phải là số dương")
    private BigDecimal discountRate;

    @NotNull(message = "appliedYear không được để trống")
    private Integer appliedYear;
}

package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandQuotaUpdateRequestDTO {
    @NotNull(message = "Hạn mức đất ở không được để trống")
    @PositiveOrZero(message = "Hạn mức đất ở phải là số không âm")
    private BigDecimal landQuota;
}

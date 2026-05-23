package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandPriceHistoryResponseDTO {
    private Integer priceId;
    private Integer landTypeId;
    private Integer areaId;
    private BigDecimal unitPrice;
    private LocalDate appliedFrom;
}

package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LandQuotaResponseDTO {
    private Integer areaId;
    private String districtCode;
    private String wardCode;
    private String streetName;
    private Integer positionLevel;
    private BigDecimal landQuota;
}

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
public class LandParcelDTO {
    private Integer landParcelId;
    private Integer landTypeId;
    private Integer areaId;
    private String parcelNumber;
    private String mapSheetNumber;
    private BigDecimal areaSize;
    private String usageDuration;
    private String usageType;
    private String usageOrigin;
    private String address;
    private String certificateNumber;
    private String gcnBookNumber;
    private String attachedHouse;
    private String attachedOther;
    private String landInfoPdf;
    private String notes;
    private String ownerCccd;
}

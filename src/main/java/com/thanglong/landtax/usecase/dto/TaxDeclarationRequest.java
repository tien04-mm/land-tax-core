package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * DTO cho yeu cau nop to khai thue dat.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationRequest {

    @NotNull(message = "Ma thua dat khong duoc de trong")
    private Integer parcelId;

    @NotNull(message = "Dien tich khai bao khong duoc de trong")
    private java.math.BigDecimal declaredArea;

    /**
     * Danh sach ID tai lieu dinh kem da upload.
     */
    private List<Long> attachmentIds;
}

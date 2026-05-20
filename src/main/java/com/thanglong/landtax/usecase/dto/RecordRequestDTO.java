package com.thanglong.landtax.usecase.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * DTO cho viec tao ho so moi (POST /api/records).
 * Chua object long nhau taxDeclaration de tuan thu chuan 3NF.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordRequestDTO {

    private Integer citizenId;
    private Integer landParcelId;
    private String recordCategory;

    /** Object long nhau — du lieu ke khai thue (luu vao bang tax_declarations) */
    private TaxDeclarationDTO taxDeclaration;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxDeclarationDTO {
        private BigDecimal declaredArea;
        private String declaredPurpose;
        private String phoneNumber;
        private String address;
    }
}

package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * DTO cho yêu cầu nộp tờ khai thuế đất.
 * Người dân chỉ cần cung cấp parcelId và danh sách các attachmentIds.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxDeclarationRequest {

    @NotNull(message = "Mã thửa đất không được để trống")
    private Integer parcelId;

    /**
     * Danh sách ID tài liệu đính kèm đã upload.
     */
    private List<Long> attachmentIds;
}

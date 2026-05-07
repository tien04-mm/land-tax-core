package com.thanglong.landtax.usecase.dto;

import lombok.*;

/**
 * DTO cho yêu cầu duyệt/từ chối tờ khai.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDeclarationRequest {

    private String processorNotes;              // Ghi chú của cán bộ duyệt (bắt buộc khi reject)
}

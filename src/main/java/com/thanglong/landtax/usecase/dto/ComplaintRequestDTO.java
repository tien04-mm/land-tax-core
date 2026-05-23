package com.thanglong.landtax.usecase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintRequestDTO {
    private Integer recordId;

    @NotBlank(message = "Nội dung khiếu nại không được để trống")
    private String content;
}

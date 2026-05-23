package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponseDTO {
    private Integer id;
    private Integer citizenId;
    private Integer recordId;
    private String content;
    private String status;
    private String responseNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

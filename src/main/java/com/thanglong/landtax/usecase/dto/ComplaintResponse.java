package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {
    private Integer id;
    private String cccdNumber;
    private String title;
    private String content;
    private Integer taxDeclarationId;
    private String status;
    private String adminResponse;
    private LocalDateTime createdAt;
}

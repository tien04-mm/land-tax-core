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
public class AuditLogResponseDTO {
    private Integer plogId;
    private Integer recordId;
    private Integer processorAccountId;
    private String processingStep;
    private String oldStatus;
    private String newStatus;
    private String processorNotes;
    private LocalDateTime processedAt;
}

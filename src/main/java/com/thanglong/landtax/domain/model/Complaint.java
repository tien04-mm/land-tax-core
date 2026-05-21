package com.thanglong.landtax.domain.model;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Domain model đại diện cho Khiếu nại (Complaint).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

    private Integer id;
    private Integer citizenId;
    private Integer recordId;
    private String content;
    private String status;           // PENDING, RESOLVED
    private String responseNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

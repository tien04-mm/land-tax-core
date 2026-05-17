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
public class MutationResponseDTO {
    private Long id;
    private Integer parcelId;
    private String requesterCccd;
    private String mutationType;
    private String status;
    private String note;
    private LocalDateTime createdAt;
}

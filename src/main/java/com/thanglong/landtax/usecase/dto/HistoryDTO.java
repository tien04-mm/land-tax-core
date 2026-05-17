package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryDTO {
    private Long id;
    private Integer parcelId;
    private String oldOwnerCccd;
    private String newOwnerCccd;
    private LocalDate mutationDate;
    private String status;
    private String note;
}

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
public class DelegationDTO {
    private Integer id;
    private Integer delegatorAccountId;
    private Integer delegateeAccountId;
    private Integer delegatedRoleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
}

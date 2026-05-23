package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    private Integer citizenId;
    private String cccdNumber;
    private String fullName;
    private String email;
    private String phoneNumber;
    private List<String> roles;
    private String activeRole;
}

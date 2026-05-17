package com.thanglong.landtax.usecase.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    private String cccdNumber;
    private String fullName;
    private String phoneNumber;
    private Integer roleId;
}

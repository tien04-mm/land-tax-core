package com.thanglong.landtax.usecase.dto;

import lombok.*;

/**
 * DTO cho yeu cau duyet/tu choi to khai.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDeclarationRequest {

    private String processorNotes;              // Ghi chu cua can bo duyet (bat buoc khi reject)
}

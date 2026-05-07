package com.thanglong.landtax.usecase.mapper;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ComplaintEntity;
import com.thanglong.landtax.usecase.dto.ComplaintResponse;
import org.springframework.stereotype.Component;

@Component
public class ComplaintMapper {

    public ComplaintResponse toResponse(ComplaintEntity entity) {
        if (entity == null) return null;

        return ComplaintResponse.builder()
                .id(entity.getId())
                .cccdNumber(entity.getCccdNumber())
                .title(entity.getTitle())
                .content(entity.getContent())
                .taxDeclarationId(entity.getTaxDeclarationId())
                .status(entity.getStatus())
                .adminResponse(entity.getAdminResponse())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

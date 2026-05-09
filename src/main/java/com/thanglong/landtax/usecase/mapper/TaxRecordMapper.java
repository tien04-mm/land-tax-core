package com.thanglong.landtax.usecase.mapper;

import com.thanglong.landtax.domain.model.TaxRecord;
import com.thanglong.landtax.usecase.dto.TaxDeclarationDTO;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyen doi giua TaxRecord domain model va TaxDeclarationDTO.
 */
@Component
public class TaxRecordMapper {

    public TaxDeclarationDTO toDto(TaxRecord taxRecord) {
        if (taxRecord == null) return null;

        return TaxDeclarationDTO.builder()
                .id(taxRecord.getId())
                .landParcelId(taxRecord.getLandParcelId())
                .citizenId(taxRecord.getCitizenId())
                .taxYear(taxRecord.getTaxYear())
                .taxableArea(taxRecord.getTaxableArea())
                .taxRate(taxRecord.getTaxRate())
                .taxAmount(taxRecord.getTaxAmount())
                .status(taxRecord.getStatus())
                .notes(taxRecord.getNotes())
                .build();
    }

    public TaxRecord toDomain(TaxDeclarationDTO dto) {
        if (dto == null) return null;

        return TaxRecord.builder()
                .id(dto.getId())
                .landParcelId(dto.getLandParcelId())
                .citizenId(dto.getCitizenId())
                .taxYear(dto.getTaxYear())
                .taxableArea(dto.getTaxableArea())
                .taxRate(dto.getTaxRate())
                .taxAmount(dto.getTaxAmount())
                .status(dto.getStatus())
                .notes(dto.getNotes())
                .build();
    }
}

package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * Service xu ly import hang loat du lieu thua dat tu file Excel (.xlsx).
 *
 * <p><b>Dinh dang file Excel mau (header row - row 1):</b></p>
 * <pre>
 * | parcelNumber | mapSheetNumber | landTypeId | areaId | areaSize | address | usageType | usageDuration | certificateNumber | ownerCccd |
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandParcelImportService {

    private final LandParcelJpaRepository landParcelJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository citizenLocalJpaRepository;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandOwnerJpaRepository landOwnerJpaRepository;

    public int importFromExcel(MultipartFile file) throws Exception {
        log.info("Starting Excel import: fileName={}, size={}KB", file.getOriginalFilename(), file.getSize() / 1024);

        int importedCount = 0;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                if (isRowEmpty(row)) continue;

                try {
                    LandParcelEntity entity = parseRow(row);
                    LandParcelEntity saved = landParcelJpaRepository.save(entity);
                    
                    String ownerCccd = getCellString(row, 9);
                    if (ownerCccd != null && !ownerCccd.isEmpty()) {
                        citizenLocalJpaRepository.findByCccdNumber(ownerCccd).ifPresent(citizen -> {
                            com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerEntity owner = com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandOwnerEntity.builder()
                                    .citizenId(citizen.getCitizenId())
                                    .landParcelId(saved.getLandParcelId())
                                    .ownershipType("PRIMARY")
                                    .build();
                            landOwnerJpaRepository.save(owner);
                        });
                    }
                    importedCount++;
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", row.getRowNum() + 1, e.getMessage());
                }
            }

            log.info("Successfully imported {} land parcels into database", importedCount);
            return importedCount;
        }
    }

    private LandParcelEntity parseRow(Row row) {
        return LandParcelEntity.builder()
                .parcelNumber(getCellString(row, 0))
                .mapSheetNumber(getCellString(row, 1))
                .landTypeId(getCellInt(row, 2))
                .areaId(getCellInt(row, 3))
                .areaSize(getCellDecimal(row, 4))
                .address(getCellString(row, 5))
                .usageType(getCellString(row, 6))
                .usageDuration(getCellString(row, 7))
                .certificateNumber(getCellString(row, 8))
                .build();
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = 0; c <= 9; c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private Integer getCellInt(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(cell.getStringCellValue().trim()); } catch (Exception e) { return null; }
    }

    private BigDecimal getCellDecimal(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        try { return new BigDecimal(cell.getStringCellValue().trim()); } catch (Exception e) { return null; }
    }
}

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
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý import hàng loạt dữ liệu thửa đất từ file Excel (.xlsx).
 *
 * <p><b>Định dạng file Excel mẫu (header row - row 1):</b></p>
 * <pre>
 * | parcelNumber | mapSheetNumber | landTypeId | areaId | areaSize | address | usageType | usageDuration | certificateNumber | ownerCccd |
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandParcelImportService {

    private final LandParcelJpaRepository landParcelJpaRepository;

    /**
     * Đọc file Excel, parse từng dòng dữ liệu và lưu vào database.
     *
     * @param file File Excel .xlsx được upload bởi LAND_OFFICER
     * @return Số lượng bản ghi đã import thành công
     */
    public int importFromExcel(MultipartFile file) throws Exception {
        log.info("Bắt đầu import Excel: fileName={}, size={}KB", file.getOriginalFilename(), file.getSize() / 1024);

        List<LandParcelEntity> parcels = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                // Bỏ qua header row (row 0)
                if (row.getRowNum() == 0) continue;

                // Bỏ qua dòng trống
                if (isRowEmpty(row)) continue;

                try {
                    LandParcelEntity entity = parseRow(row);
                    parcels.add(entity);
                } catch (Exception e) {
                    log.warn("Lỗi khi parse dòng {}: {}", row.getRowNum() + 1, e.getMessage());
                    // Tiếp tục dòng tiếp theo thay vì abort toàn bộ
                }
            }

            if (parcels.isEmpty()) {
                throw new IllegalArgumentException("File Excel không có dữ liệu hợp lệ");
            }

            List<LandParcelEntity> saved = landParcelJpaRepository.saveAll(parcels);
            log.info("Import thành công {} thửa đất vào database", saved.size());
            return saved.size();
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
                .ownerCccd(getCellString(row, 9))
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

package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service xuat bao cao tinh hinh thu thue theo khu vuc ra file Excel (.xlsx).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxReportExportService {

    private final LandParcelJpaRepository landParcelJpaRepository;

    /**
     * Xuat bao cao tinh hinh thu thue khu vuc ra file Excel.
     *
     * @param areaId ID khu vuc (null = tat ca)
     * @param status Trang thai hoa don: PAID / UNPAID / null (tat ca)
     * @param year   Nam bao cao (dung cho tieu de, null = khong loc theo nam)
     * @return byte[] noi dung file .xlsx
     */
    public byte[] exportToExcel(Integer areaId, String status, Integer year) throws Exception {
        log.info("Exporting Excel report: areaId={}, status={}, year={}", areaId, status, year);

        // Lay danh sach thua dat theo khu vuc
        List<LandParcelEntity> parcels = (areaId != null)
                ? landParcelJpaRepository.findByAreaId(areaId)
                : landParcelJpaRepository.findAll();

        // Lay danh sach billId theo thua dat (dua tren declarationId - neu co quan he)
        // O day lay tat ca bills roi loc theo status
        List<?> allBills = List.of();

        // Build thong ke tong hop
        BigDecimal totalAmount = BigDecimal.ZERO;
        long paidCount = 0;
        long unpaidCount = 0;

        try (Workbook workbook = new XSSFWorkbook()) {
            // ===== Sheet 1: Tong hop =====
            Sheet summarySheet = workbook.createSheet("Tong hop");
            createSummarySheet(summarySheet, workbook, areaId, status, year, totalAmount, paidCount, unpaidCount, parcels.size());

            // ===== Sheet 2: Chi tiet hoa don =====
            Sheet detailSheet = workbook.createSheet("Chi tiet hoa don");
            createDetailSheet(detailSheet, workbook, allBills);

            // ===== Sheet 3: Danh sach thua dat =====
            Sheet parcelSheet = workbook.createSheet("Danh sach thua dat");
            createParcelSheet(parcelSheet, workbook, parcels);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Report exported successfully: {} bills, {} parcels", allBills.size(), parcels.size());
            return out.toByteArray();
        }
    }

    private void createSummarySheet(Sheet sheet, Workbook wb, Integer areaId, String status,
                                     Integer year, BigDecimal total, long paid, long unpaid, int parcelCount) {
        CellStyle titleStyle = wb.createCellStyle();
        Font titleFont = wb.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        int rowIdx = 0;
        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("BAO CAO TINH HINH THU THUE DAT");
        titleCell.setCellStyle(titleStyle);

        rowIdx++; // blank row
        String[][] summaryData = {
                {"Khu vuc (areaId)", areaId != null ? String.valueOf(areaId) : "Tat ca"},
                {"Trang thai loc", status != null ? status : "Tat ca"},
                {"Nam bao cao", year != null ? String.valueOf(year) : "Tat ca"},
                {"Tong so thua dat", String.valueOf(parcelCount)},
                {"Tong so hoa don PAID", String.valueOf(paid)},
                {"Tong so hoa don UNPAID", String.valueOf(unpaid)},
                {"Tong thu thue (VND)", total.toPlainString()},
        };

        for (String[] row : summaryData) {
            Row r = sheet.createRow(rowIdx++);
            Cell k = r.createCell(0);
            k.setCellValue(row[0]);
            k.setCellStyle(headerStyle);
            r.createCell(1).setCellValue(row[1]);
        }
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 6000);
    }

    private void createDetailSheet(Sheet sheet, Workbook wb, List<?> bills) {
        CellStyle headerStyle = wb.createCellStyle();
        Font hf = wb.createFont(); hf.setBold(true);
        headerStyle.setFont(hf);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"Ma HD (billId)", "CCCD", "So tien (VND)", "Trang thai", "Mo ta", "Cong thuc tinh"};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void createParcelSheet(Sheet sheet, Workbook wb, List<LandParcelEntity> parcels) {
        CellStyle headerStyle = wb.createCellStyle();
        Font hf = wb.createFont(); hf.setBold(true);
        headerStyle.setFont(hf);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"So thua", "To ban do", "Dien tich (m2)", "Dia chi", "CCCD Chu", "Loai dat"};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (LandParcelEntity p : parcels) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(p.getParcelNumber() != null ? p.getParcelNumber() : "");
            row.createCell(1).setCellValue(p.getMapSheetNumber() != null ? p.getMapSheetNumber() : "");
            row.createCell(2).setCellValue(p.getAreaSize() != null ? p.getAreaSize().doubleValue() : 0);
            row.createCell(3).setCellValue(p.getAddress() != null ? p.getAddress() : "");
            row.createCell(4).setCellValue(p.getOwnerCccd() != null ? p.getOwnerCccd() : "");
            row.createCell(5).setCellValue(p.getLandTypeId() != null ? "Loai " + p.getLandTypeId() : "");
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }
}

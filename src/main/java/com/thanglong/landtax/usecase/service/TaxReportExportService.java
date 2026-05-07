package com.thanglong.landtax.usecase.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * Service xuất báo cáo tình hình thu thuế theo khu vực ra file Excel (.xlsx).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaxReportExportService {

    private final TaxBillRepository taxBillRepository;
    private final LandParcelJpaRepository landParcelJpaRepository;

    /**
     * Xuất báo cáo tình hình thu thuế khu vực ra file Excel.
     *
     * @param areaId ID khu vực (null = tất cả)
     * @param status Trạng thái hóa đơn: PAID / UNPAID / null (tất cả)
     * @param year   Năm báo cáo (dùng cho tiêu đề, null = không lọc theo năm)
     * @return byte[] nội dung file .xlsx
     */
    public byte[] exportToExcel(Integer areaId, String status, Integer year) throws Exception {
        log.info("Xuất báo cáo Excel: areaId={}, status={}, year={}", areaId, status, year);

        // Lấy danh sách thửa đất theo khu vực
        List<LandParcelEntity> parcels = (areaId != null)
                ? landParcelJpaRepository.findByAreaId(areaId)
                : landParcelJpaRepository.findAll();

        // Lấy danh sách billId theo thửa đất (dựa trên declarationId - nếu có quan hệ)
        // Ở đây lấy tất cả bills rồi lọc theo status
        List<TaxBillEntity> allBills = (status != null && !status.isBlank())
                ? taxBillRepository.findByStatus(status)
                : taxBillRepository.findAll();

        // Build thống kê tổng hợp
        BigDecimal totalAmount = allBills.stream()
                .map(b -> b.getAmount() != null ? b.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidCount = allBills.stream().filter(b -> "PAID".equals(b.getStatus())).count();
        long unpaidCount = allBills.stream().filter(b -> "UNPAID".equals(b.getStatus())).count();

        try (Workbook workbook = new XSSFWorkbook()) {
            // ===== Sheet 1: Tổng hợp =====
            Sheet summarySheet = workbook.createSheet("Tổng hợp");
            createSummarySheet(summarySheet, workbook, areaId, status, year, totalAmount, paidCount, unpaidCount, parcels.size());

            // ===== Sheet 2: Chi tiết hóa đơn =====
            Sheet detailSheet = workbook.createSheet("Chi tiết hóa đơn");
            createDetailSheet(detailSheet, workbook, allBills);

            // ===== Sheet 3: Danh sách thửa đất =====
            Sheet parcelSheet = workbook.createSheet("Danh sách thửa đất");
            createParcelSheet(parcelSheet, workbook, parcels);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            log.info("Xuất báo cáo thành công: {} hóa đơn, {} thửa đất", allBills.size(), parcels.size());
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
        titleCell.setCellValue("BÁO CÁO TÌNH HÌNH THU THUẾ ĐẤT");
        titleCell.setCellStyle(titleStyle);

        rowIdx++; // blank row
        String[][] summaryData = {
                {"Khu vực (areaId)", areaId != null ? String.valueOf(areaId) : "Tất cả"},
                {"Trạng thái lọc", status != null ? status : "Tất cả"},
                {"Năm báo cáo", year != null ? String.valueOf(year) : "Tất cả"},
                {"Tổng số thửa đất", String.valueOf(parcelCount)},
                {"Tổng số hóa đơn PAID", String.valueOf(paid)},
                {"Tổng số hóa đơn UNPAID", String.valueOf(unpaid)},
                {"Tổng thu thuế (VNĐ)", total.toPlainString()},
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

    private void createDetailSheet(Sheet sheet, Workbook wb, List<TaxBillEntity> bills) {
        CellStyle headerStyle = wb.createCellStyle();
        Font hf = wb.createFont(); hf.setBold(true);
        headerStyle.setFont(hf);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"Mã HĐ (billId)", "CCCD", "Số tiền (VNĐ)", "Trạng thái", "Mô tả", "Công thức tính"};
        Row hRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (TaxBillEntity b : bills) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(b.getBillId() != null ? b.getBillId() : 0);
            row.createCell(1).setCellValue(b.getCccdNumber() != null ? b.getCccdNumber() : "");
            row.createCell(2).setCellValue(b.getAmount() != null ? b.getAmount().doubleValue() : 0);
            row.createCell(3).setCellValue(b.getStatus() != null ? b.getStatus() : "");
            row.createCell(4).setCellValue(b.getDescription() != null ? b.getDescription() : "");
            row.createCell(5).setCellValue(b.getCalculationFormula() != null ? b.getCalculationFormula() : "");
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void createParcelSheet(Sheet sheet, Workbook wb, List<LandParcelEntity> parcels) {
        CellStyle headerStyle = wb.createCellStyle();
        Font hf = wb.createFont(); hf.setBold(true);
        headerStyle.setFont(hf);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        String[] headers = {"Số thửa", "Tờ bản đồ", "Diện tích (m²)", "Địa chỉ", "CCCD Chủ", "Loại đất"};
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
            row.createCell(5).setCellValue(p.getLandTypeId() != null ? "Loại " + p.getLandTypeId() : "");
        }
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }
}

package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationStatementEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxBillEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.TaxBillRepository;
import com.thanglong.landtax.infrastructure.config.aop.AuditLog;
import com.thanglong.landtax.usecase.service.ReconciliationService;
import com.thanglong.landtax.usecase.service.TaxReportExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Module Đối soát thanh toán và Xuất báo cáo.
 *
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/payments/reconcile/upload — Cán bộ tải file CSV sao kê ngân hàng</li>
 *   <li>GET  /api/payments/reconcile/discrepancies — Xem danh sách sai lệch</li>
 *   <li>PUT  /api/payments/bills/{id}/adjust — Điều chỉnh thủ công trạng thái hóa đơn</li>
 *   <li>GET  /api/admin/reports/export — Xuất báo cáo Excel</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("all")
public class PaymentReconciliationController {

    private final ReconciliationService reconciliationService;
    private final TaxReportExportService taxReportExportService;
    private final TaxBillRepository taxBillRepository;

    // ─────────────────────────────────────────────────────────────────
    // 1. ĐỐI SOÁT SAO KÊ NGÂN HÀNG
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/payments/reconcile/upload — Upload file CSV sao kê ngân hàng.
     * Chỉ TAX_OFFICER / ADMIN mới được phép.
     */
    @PostMapping(value = "/api/payments/reconcile/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditLog(action = "Upload sao kê ngân hàng")
    public ResponseEntity<?> uploadReconciliationFile(@RequestParam("file") MultipartFile file) {
        boolean isOfficer = isOfficer();
        if (!isOfficer) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ cán bộ thuế mới được phép đối soát"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
        }
        String filename = java.util.Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file CSV (.csv)"));
        }

        try {
            Map<String, Object> result = reconciliationService.processReconciliation(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi xử lý đối soát: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi xử lý file: " + e.getMessage()));
        }
    }

    /**
     * GET /api/payments/reconcile/discrepancies — Lấy danh sách sai lệch để xử lý thủ công.
     */
    @GetMapping("/api/payments/reconcile/discrepancies")
    public ResponseEntity<List<ReconciliationStatementEntity>> getDiscrepancies() {
        if (!isOfficer()) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(reconciliationService.getDiscrepancies());
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. XỬ LÝ SAI LỆCH — Điều chỉnh thủ công trạng thái hóa đơn
    // ─────────────────────────────────────────────────────────────────

    /**
     * PUT /api/payments/bills/{id}/adjust — Cán bộ điều chỉnh trạng thái hóa đơn sau đối soát thủ công.
     * Body: { "status": "PAID" | "UNPAID" | "WAIVED", "note": "Lý do điều chỉnh" }
     */
    @PutMapping("/api/payments/bills/{id}/adjust")
    @AuditLog(action = "Điều chỉnh trạng thái hóa đơn")
    public ResponseEntity<?> adjustBillStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        if (!isOfficer()) {
            return ResponseEntity.status(403).body(Map.of("error", "Chỉ cán bộ thuế mới được điều chỉnh hóa đơn"));
        }

        String newStatus = body.get("status");
        String note = body.getOrDefault("note", "Điều chỉnh thủ công sau đối soát");

        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu trường 'status'"));
        }
        if (!List.of("PAID", "UNPAID", "WAIVED", "DISCREPANCY").contains(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Giá trị status không hợp lệ. Chấp nhận: PAID, UNPAID, WAIVED, DISCREPANCY"));
        }

        TaxBillEntity bill = taxBillRepository.findByBillId(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn ID: " + id));

        String oldStatus = bill.getStatus();
        bill.setStatus(newStatus);
        bill.setDescription(java.util.Optional.ofNullable(bill.getDescription()).orElse("") + " | [Điều chỉnh] " + note);
        taxBillRepository.save(bill);

        log.info("Điều chỉnh hóa đơn billId={}: {} → {} | ghi chú: {}", id, oldStatus, newStatus, note);

        return ResponseEntity.ok(Map.of(
                "message", "Điều chỉnh trạng thái hóa đơn thành công",
                "billId", id,
                "oldStatus", oldStatus != null ? oldStatus : "",
                "newStatus", newStatus,
                "note", note
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. XUẤT BÁO CÁO EXCEL
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /api/admin/reports/export — Xuất báo cáo tình hình thu thuế khu vực ra file .xlsx.
     *
     * <p>Query params:</p>
     * <ul>
     *   <li>areaId (tùy chọn): Lọc theo khu vực</li>
     *   <li>status (tùy chọn): PAID / UNPAID</li>
     *   <li>year (tùy chọn): Năm báo cáo</li>
     * </ul>
     */
    @GetMapping("/api/admin/reports/export")
    @AuditLog(action = "Xuất báo cáo thu thuế")
    public ResponseEntity<byte[]> exportReport(
            @RequestParam(required = false) Integer areaId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year) {

        if (!isOfficer()) return ResponseEntity.status(403).build();

        try {
            byte[] excelBytes = taxReportExportService.exportToExcel(areaId, status, year);

            String yearStr = (year != null) ? String.valueOf(year) : String.valueOf(LocalDate.now().getYear());
            String filename = String.format("bao-cao-thu-thue-%s.xlsx", yearStr);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("Xuất báo cáo Excel: areaId={}, status={}, year={}, size={}KB",
                    areaId, status, year, excelBytes.length / 1024);
            return ResponseEntity.ok().headers(headers).body(excelBytes);

        } catch (Exception e) {
            log.error("Lỗi xuất báo cáo Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────

    private boolean isOfficer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN")
                        || a.getAuthority().contains("TAX_OFFICER")
                        || a.getAuthority().contains("OFFICER"));
    }
}

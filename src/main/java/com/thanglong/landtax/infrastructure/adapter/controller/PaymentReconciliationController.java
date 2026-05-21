package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.ReconciliationLogEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.ReconciliationLogJpaRepository;
import com.thanglong.landtax.infrastructure.config.aop.AuditLog;
import com.thanglong.landtax.usecase.service.TaxReportExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Controller cho Module Doi soat thanh toan va Xuat bao cao.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("all")
public class PaymentReconciliationController {

    private final TaxReportExportService taxReportExportService;
    private final ReconciliationLogJpaRepository reconciliationLogJpaRepository;

    /**
     * GET  /api/payments/reconcile/discrepancies  Xem danh sach sai lech (sau khi doi soat webhook tu dong).
     */
    @GetMapping("/api/payments/reconcile/discrepancies")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER')")
    public ResponseEntity<List<ReconciliationLogEntity>> getDiscrepancies() {
        log.info("GET /api/payments/reconcile/discrepancies - Lay danh sach giao dich sai lech");
        return ResponseEntity.ok(reconciliationLogJpaRepository.findByStatusOrderByCreatedAtDesc("DISCREPANCY"));
    }

    /**
     * PUT /api/payments/bills/{id}/adjust  Can bo dieu chinh trang thai hoa don sau doi soat thu cong.
     * Body: { "status": "PAID" | "UNPAID" | "WAIVED", "note": "Ly do dieu chinh" }
     */
    @PutMapping("/api/payments/bills/{id}/adjust")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER')")
    @AuditLog(action = "Dieu chinh trang thai hoa don")
    public ResponseEntity<?> adjustBillStatus(@PathVariable Integer id, @RequestBody Map<String, String> body) {

        String newStatus = body.get("status");
        String note = body.getOrDefault("note", "Dieu chinh thu cong sau doi soat");

        if (newStatus == null || newStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thieu truong 'status'"));
        }
        if (!List.of("PAID", "UNPAID", "WAIVED", "DISCREPANCY").contains(newStatus)) {
            return ResponseEntity.badRequest().body(Map.of(
                     "error", "Gia tri status khong hop le. Chap nhan: PAID, UNPAID, WAIVED, DISCREPANCY"));
        }

        log.info("Mock dieu chinh hoa don billId={}: status={} | ghi chu: {}", id, newStatus, note);

        return ResponseEntity.ok(Map.of(
                "message", "Dieu chinh trang thai hoa don thanh cong",
                "billId", id,
                "oldStatus", "UNPAID",
                "newStatus", newStatus,
                "note", note
        ));
    }

    /**
     * GET /api/admin/reports/export  Xuat bao cao tinh hinh thu thue khu vuc ra file .xlsx.
     */
    @GetMapping("/api/admin/reports/export")
    @AuditLog(action = "Xuat bao cao thu thue")
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

            log.info("Xuat bao cao Excel: areaId={}, status={}, year={}, size={}KB",
                    areaId, status, year, excelBytes.length / 1024);
            return ResponseEntity.ok().headers(headers).body(excelBytes);

        } catch (Exception e) {
            log.error("Loi xuat bao cao Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isOfficer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN")
                        || a.getAuthority().contains("TAX_OFFICER")
                        || a.getAuthority().contains("OFFICER"));
    }
}

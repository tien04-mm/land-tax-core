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
 * Controller cho Module Doi soat thanh toan va Xuat bao cao.
 *
 * <p><b>Endpoints:</b></p>
 * <ul>
 *   <li>POST /api/payments/reconcile/upload  Can bo tai file CSV sao ke ngan hang</li>
 *   <li>GET  /api/payments/reconcile/discrepancies  Xem danh sach sai lech</li>
 *   <li>PUT  /api/payments/bills/{id}/adjust  Dieu chinh thu cong trang thai hoa don</li>
 *   <li>GET  /api/admin/reports/export  Xuat bao cao Excel</li>
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

    // 
    // 1. DOI SOAT SAO KE NGAN HANG
    // 

    /**
     * POST /api/payments/reconcile/upload  Upload file CSV sao ke ngan hang.
     * Chi TAX_OFFICER / ADMIN moi duoc phep.
     */
    @PostMapping(value = "/api/payments/reconcile/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER')")
    @AuditLog(action = "Upload sao ke ngan hang")
    public ResponseEntity<?> uploadReconciliationFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File khong duoc de trong"));
        }
        String filename = java.util.Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chi chap nhan file CSV (.csv)"));
        }

        try {
            Map<String, Object> result = reconciliationService.processReconciliation(file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Loi xu ly doi soat: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Loi xu ly file: " + e.getMessage()));
        }
    }

    /**
     * GET /api/payments/reconcile/discrepancies  Lay danh sach sai lech de xu ly thu cong.
     */
    @GetMapping("/api/payments/reconcile/discrepancies")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'TAX_OFFICER')")
    public ResponseEntity<List<ReconciliationStatementEntity>> getDiscrepancies() {
        return ResponseEntity.ok(reconciliationService.getDiscrepancies());
    }

    // 
    // 2. XU LY SAI LECH  Dieu chinh thu cong trang thai hoa don
    // 

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

        TaxBillEntity bill = taxBillRepository.findByBillId(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay hoa don ID: " + id));

        String oldStatus = bill.getStatus();
        bill.setStatus(newStatus);
        bill.setDescription(java.util.Optional.ofNullable(bill.getDescription()).orElse("") + " | [Dieu chinh] " + note);
        taxBillRepository.save(bill);

        log.info("Dieu chinh hoa don billId={}: {}  {} | ghi chu: {}", id, oldStatus, newStatus, note);

        return ResponseEntity.ok(Map.of(
                "message", "Dieu chinh trang thai hoa don thanh cong",
                "billId", id,
                "oldStatus", oldStatus != null ? oldStatus : "",
                "newStatus", newStatus,
                "note", note
        ));
    }

    // 
    // 3. XUAT BAO CAO EXCEL
    // 

    /**
     * GET /api/admin/reports/export  Xuat bao cao tinh hinh thu thue khu vuc ra file .xlsx.
     *
     * <p>Query params:</p>
     * <ul>
     *   <li>areaId (tuy chon): Loc theo khu vuc</li>
     *   <li>status (tuy chon): PAID / UNPAID</li>
     *   <li>year (tuy chon): Nam bao cao</li>
     * </ul>
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

    // 
    // Helper
    // 

    private boolean isOfficer() {
        return org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN")
                        || a.getAuthority().contains("TAX_OFFICER")
                        || a.getAuthority().contains("OFFICER"));
    }
}

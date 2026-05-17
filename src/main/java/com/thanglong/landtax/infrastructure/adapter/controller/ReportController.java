package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.DossierStatusReportDTO;
import com.thanglong.landtax.usecase.dto.RevenueReportDTO;
import com.thanglong.landtax.usecase.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    /**
     * GET /api/reports/revenue - Báo cáo tổng doanh thu theo tháng của năm.
     */
    @GetMapping("/revenue")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TAX_OFFICER', 'ROLE_LAND_OFFICER')")
    public ResponseEntity<List<RevenueReportDTO>> getRevenueReport(
            @RequestParam(defaultValue = "2026") Integer year) {
        log.info("GET /api/reports/revenue called for year {}", year);
        return ResponseEntity.ok(reportService.getRevenueReport(year));
    }

    /**
     * GET /api/reports/dossier-status - Báo cáo số lượng hồ sơ theo từng trạng thái.
     */
    @GetMapping("/dossier-status")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TAX_OFFICER', 'ROLE_LAND_OFFICER')")
    public ResponseEntity<List<DossierStatusReportDTO>> getDossierStatusReport() {
        log.info("GET /api/reports/dossier-status called");
        return ResponseEntity.ok(reportService.getDossierStatusReport());
    }
}

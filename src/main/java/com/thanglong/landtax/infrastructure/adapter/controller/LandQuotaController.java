package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.LandQuotaResponseDTO;
import com.thanglong.landtax.usecase.dto.LandQuotaUpdateRequestDTO;
import com.thanglong.landtax.usecase.service.LandQuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/areas")
@RequiredArgsConstructor
@Slf4j
public class LandQuotaController {

    private final LandQuotaService landQuotaService;

    /**
     * API xem hạn mức đất ở của một khu vực.
     */
    @GetMapping("/{areaId}/quota")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER', 'TAX_OFFICER', 'CITIZEN')")
    public ResponseEntity<LandQuotaResponseDTO> getLandQuota(@PathVariable Integer areaId) {
        log.info("GET /api/admin/areas/{}/quota", areaId);
        LandQuotaResponseDTO response = landQuotaService.getLandQuota(areaId);
        return ResponseEntity.ok(response);
    }

    /**
     * API cập nhật hạn mức đất ở cho khu vực, yêu cầu quyền ROLE_ADMIN hoặc ROLE_LAND_OFFICER.
     */
    @PutMapping("/{areaId}/quota")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<LandQuotaResponseDTO> updateLandQuota(
            @PathVariable Integer areaId,
            @Valid @RequestBody LandQuotaUpdateRequestDTO request) {
        log.info("PUT /api/admin/areas/{}/quota - landQuota={}", areaId, request.getLandQuota());
        LandQuotaResponseDTO response = landQuotaService.updateLandQuota(areaId, request.getLandQuota());
        return ResponseEntity.ok(response);
    }
}

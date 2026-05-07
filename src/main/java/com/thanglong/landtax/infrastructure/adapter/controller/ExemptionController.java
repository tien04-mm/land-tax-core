package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.TaxExemptSubjectEntity;
import com.thanglong.landtax.usecase.service.ExemptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/exemptions")
@RequiredArgsConstructor
public class ExemptionController {

    private final ExemptionService exemptionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER')")
    public ResponseEntity<?> createExemption(@RequestBody Map<String, Object> request) {
        try {
            String uploaderCccd = SecurityContextHolder.getContext().getAuthentication().getName();
            TaxExemptSubjectEntity exemption = exemptionService.createExemption(uploaderCccd, request);
            return ResponseEntity.ok(Map.of(
                    "data", exemption,
                    "message", "Cập nhật danh sách miễn giảm thành công"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

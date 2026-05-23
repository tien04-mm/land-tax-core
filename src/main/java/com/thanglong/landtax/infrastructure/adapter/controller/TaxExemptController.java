package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.usecase.dto.TaxExemptApproveRequestDTO;
import com.thanglong.landtax.usecase.dto.TaxExemptRequestDTO;
import com.thanglong.landtax.usecase.dto.TaxExemptSubjectResponseDTO;
import com.thanglong.landtax.usecase.service.TaxExemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tax/exemptions")
@RequiredArgsConstructor
@Slf4j
public class TaxExemptController {

    private final TaxExemptService taxExemptService;

    /**
     * Công dân nộp yêu cầu miễn giảm thuế.
     */
    @PostMapping
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<TaxExemptSubjectResponseDTO> submitExemption(
            @Valid @RequestBody TaxExemptRequestDTO request) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("POST /api/tax/exemptions - citizen cccd={}", cccd);
        TaxExemptSubjectResponseDTO response = taxExemptService.submitExemption(request, cccd);
        return ResponseEntity.ok(response);
    }

    /**
     * Công dân xem danh sách miễn giảm của mình.
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<List<TaxExemptSubjectResponseDTO>> getMyExemptions() {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("GET /api/tax/exemptions/me - citizen cccd={}", cccd);
        List<TaxExemptSubjectResponseDTO> response = taxExemptService.getExexemptionsByCitizen(cccd);
        return ResponseEntity.ok(response);
    }

    /**
     * Cán bộ duyệt hoặc admin xem danh sách miễn giảm thuế.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TAX_OFFICER', 'ADMIN')")
    public ResponseEntity<List<TaxExemptSubjectResponseDTO>> getTaxExemptions(
            @RequestParam(value = "applied_year", required = false) Integer appliedYear,
            @RequestParam(value = "status", required = false) String status) {
        log.info("GET /api/tax/exemptions - applied_year={}, status={}", appliedYear, status);
        List<TaxExemptSubjectResponseDTO> exemptions = taxExemptService.getTaxExemptions(appliedYear, status);
        return ResponseEntity.ok(exemptions);
    }

    /**
     * Phê duyệt hồ sơ miễn giảm thuế.
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('TAX_OFFICER', 'ADMIN')")
    public ResponseEntity<TaxExemptSubjectResponseDTO> approveExemption(
            @PathVariable Integer id,
            @Valid @RequestBody TaxExemptApproveRequestDTO request) {
        log.info("PUT /api/tax/exemptions/{}/approve - status={}", id, request.getStatus());
        TaxExemptSubjectResponseDTO response = taxExemptService.approveExemption(id, request.getStatus());
        return ResponseEntity.ok(response);
    }
}

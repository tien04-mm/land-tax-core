package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.MutationRequestEntity;
import com.thanglong.landtax.usecase.dto.MutationResponseDTO;
import com.thanglong.landtax.usecase.service.MutationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mutation-requests")
@RequiredArgsConstructor
@Slf4j
public class MutationController {

    private final MutationService mutationService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CITIZEN')")
    public ResponseEntity<?> createMutationRequest(@RequestBody MutationRequestEntity request) {
        log.info("Cong dan nop yeu cau bien dong cho thua dat ID: {}", request.getParcelId());
        MutationRequestEntity created = mutationService.createMutationRequest(request);
        return ResponseEntity.ok(Map.of(
                "data", created,
                "message", "Nop yeu cau bien dong thanh cong"
        ));
    }

    @PutMapping("/{id}/need-more-docs")
    @PreAuthorize("hasAuthority('ROLE_LAND_OFFICER')")
    public ResponseEntity<?> needMoreDocs(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String reason = body.getOrDefault("reason", "Can bo sung ho so");
        log.info("LAND_OFFICER yeu cau bo sung ho so cho bien dong ID: {}", id);
        MutationRequestEntity updated = mutationService.needMoreDocs(id, reason);
        return ResponseEntity.ok(Map.of(
                "data", updated,
                "message", "Da gui yeu cau bo sung ho so"
        ));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ROLE_LAND_OFFICER')")
    public ResponseEntity<?> approveMutation(@PathVariable Long id) {
        log.info("LAND_OFFICER phe duyet bien dong ID: {}", id);
        MutationRequestEntity approved = mutationService.approveMutation(id);
        return ResponseEntity.ok(Map.of(
                "data", approved,
                "message", "Phe duyet bien dong thanh cong, chu so huu da duoc cap nhat"
        ));
    }

    /**
     * GET /api/mutation-requests - Lay danh sach Yeu cau bien dong dat dai.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CITIZEN', 'ROLE_LAND_OFFICER', 'ROLE_ADMIN')")
    public ResponseEntity<Page<MutationResponseDTO>> getMutationRequests(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("GET /api/mutation-requests: status={}, page={}, size={}", status, page, size);
        Page<MutationResponseDTO> responses = mutationService.getMutationRequests(status, page, size);
        return ResponseEntity.ok(responses);
    }
}

package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.usecase.service.VerifyDeclarationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
public class RecordController {

    private final RecordJpaRepository recordJpaRepository;
    private final VerifyDeclarationUseCase verifyDeclarationUseCase;

    /**
     * Lay danh sach ho so dang cho xac minh (trang thai SUBMITTED).
     */
    @GetMapping("/submitted")
    @PreAuthorize("hasRole('LAND_OFFICER')")
    public ResponseEntity<List<RecordEntity>> getSubmittedRecords() {
        log.info("GET /api/records/submitted - LAND_OFFICER lay danh sach ho so cho xac minh");
        List<RecordEntity> records = recordJpaRepository.findByCurrentStatus("SUBMITTED");
        return ResponseEntity.ok(records);
    }

    /**
     * Tiep nhan va xac minh ho so.
     */
    @PutMapping("/{id}/verify")
    @PreAuthorize("hasRole('LAND_OFFICER')")
    public ResponseEntity<?> verifyRecord(@PathVariable Integer id) {
        log.info("PUT /api/records/{}/verify", id);
        try {
            return ResponseEntity.ok(verifyDeclarationUseCase.verifyDeclaration(id));
        } catch (RuntimeException e) {
            log.error("Loi khi xac minh ho so {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

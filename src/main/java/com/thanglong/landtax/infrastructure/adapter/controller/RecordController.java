package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.usecase.dto.ForwardRecordRequest;
import com.thanglong.landtax.usecase.service.RecordService;
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
    private final RecordService recordService;

    /**
     * Lay danh sach ho so dang cho xac minh (trang thai SUBMITTED).
     */
    @GetMapping("/submitted")
    @PreAuthorize("hasAuthority('ROLE_LAND_OFFICER')")
    public ResponseEntity<List<RecordEntity>> getSubmittedRecords() {
        log.info("GET /api/records/submitted - LAND_OFFICER lay danh sach ho so cho xac minh");
        List<RecordEntity> records = recordJpaRepository.findByCurrentStatus("SUBMITTED");
        return ResponseEntity.ok(records);
    }

    /**
     * Tiep nhan va xac minh ho so.
     */
    @PutMapping("/{id}/verify")
    @PreAuthorize("hasAuthority('ROLE_LAND_OFFICER')")
    public ResponseEntity<?> verifyRecord(@PathVariable Integer id) {
        log.info("PUT /api/records/{}/verify", id);
        try {
            return ResponseEntity.ok(verifyDeclarationUseCase.verifyDeclaration(id));
        } catch (RuntimeException e) {
            log.error("Loi khi xac minh ho so {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    /**
     * Luân chuyển hồ sơ sang cơ quan Thuế.
     */
    @PostMapping("/{id}/forward")
    @PreAuthorize("hasAuthority('ROLE_LAND_OFFICER')")
    public ResponseEntity<?> forwardRecord(@PathVariable Integer id, @RequestBody ForwardRecordRequest request) {
        log.info("POST /api/records/{}/forward - LAND_OFFICER luan chuyen ho so", id);
        try {
            recordService.forwardRecord(id, request);
            return ResponseEntity.ok(java.util.Map.of("message", "Luân chuyển hồ sơ sang cơ quan Thuế thành công"));
        } catch (IllegalArgumentException e) {
            log.error("Loi khi luan chuyen ho so {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

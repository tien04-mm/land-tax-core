package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.RecordEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.RecordJpaRepository;
import com.thanglong.landtax.usecase.dto.ForwardRecordRequest;
import com.thanglong.landtax.usecase.dto.RecordRequestDTO;
import com.thanglong.landtax.usecase.service.RecordService;
import com.thanglong.landtax.usecase.service.VerifyDeclarationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RecordController {

    private final RecordJpaRepository recordJpaRepository;
    private final VerifyDeclarationUseCase verifyDeclarationUseCase;
    private final RecordService recordService;

    /**
     * Tao ho so moi (chuan 3NF: nested taxDeclaration).
     *
     * <p>Body example:</p>
     * <pre>
     * {
     *   "citizenId": 4,
     *   "landParcelId": 1,
     *   "recordCategory": "TAX_DECLARATION",
     *   "taxDeclaration": {
     *     "declaredArea": 100,
     *     "declaredPurpose": "Nha o"
     *   }
     * }
     * </pre>
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER', 'ROLE_CITIZEN')")
    public ResponseEntity<?> createRecord(@RequestBody RecordRequestDTO request) {
        log.info("POST /api/records - Tao ho so moi");
        try {
            RecordEntity saved = recordService.createRecord(request);
            return ResponseEntity.ok(buildRecordResponse(saved));
        } catch (Exception e) {
            log.error("Loi khi tao ho so: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Lay danh sach tat ca ho so.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<List<RecordEntity>> getAllRecords() {
        log.info("GET /api/records - Lay danh sach tat ca ho so");
        List<RecordEntity> records = recordJpaRepository.findAll();
        return ResponseEntity.ok(records);
    }

    /**
     * Xem chi tiet ho so (tra ve nested taxDeclaration neu co).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<?> getRecordById(@PathVariable Integer id) {
        log.info("GET /api/records/{} - Xem chi tiet ho so", id);
        return recordJpaRepository.findById(id)
                .map(record -> ResponseEntity.ok(buildRecordResponse(record)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lay danh sach ho so dang cho xac minh (trang thai SUBMITTED).
     */
    @GetMapping("/submitted")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<List<RecordEntity>> getSubmittedRecords() {
        log.info("GET /api/records/submitted - lay danh sach ho so cho xac minh");
        List<RecordEntity> records = recordJpaRepository.findByCurrentStatus("SUBMITTED");
        return ResponseEntity.ok(records);
    }

    /**
     * Tiep nhan va xac minh ho so.
     */
    @PutMapping("/{id}/verify")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<?> verifyRecord(@PathVariable Integer id) {
        log.info("PUT /api/records/{}/verify", id);
        try {
            return ResponseEntity.ok(verifyDeclarationUseCase.verifyDeclaration(id));
        } catch (RuntimeException e) {
            log.error("Loi khi xac minh ho so {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Luân chuyển hồ sơ sang cơ quan Thuế.
     */
    @PostMapping("/{id}/forward")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_LAND_OFFICER', 'ROLE_TAX_OFFICER')")
    public ResponseEntity<?> forwardRecord(@PathVariable Integer id, @RequestBody ForwardRecordRequest request) {
        log.info("POST /api/records/{}/forward - luan chuyen ho so", id);
        try {
            recordService.forwardRecord(id, request);
            return ResponseEntity.ok(Map.of("message", "Luân chuyển hồ sơ sang cơ quan Thuế thành công"));
        } catch (IllegalArgumentException e) {
            log.error("Loi khi luan chuyen ho so {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Build response voi nested taxDeclaration object (chuan 3NF).
     */
    private Map<String, Object> buildRecordResponse(RecordEntity record) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("recordId", record.getRecordId());
        response.put("citizenId", record.getCitizenId());
        response.put("landParcelId", record.getLandParcelId());
        response.put("recordCategory", record.getRecordCategory());
        response.put("currentStatus", record.getCurrentStatus());
        response.put("submittedAt", record.getSubmittedAt());

        // Nested taxDeclaration object (3NF compliance)
        if (record.getTaxDeclaration() != null) {
            var decl = record.getTaxDeclaration();
            Map<String, Object> taxDecl = new LinkedHashMap<>();
            taxDecl.put("id", decl.getDeclarationId());
            taxDecl.put("declaredArea", decl.getDeclaredArea());
            taxDecl.put("declaredPurpose", decl.getDeclaredUsage());
            taxDecl.put("reviewNote", decl.getDeclarationNotes());
            response.put("taxDeclaration", taxDecl);
        } else {
            response.put("taxDeclaration", null);
        }

        return response;
    }
}

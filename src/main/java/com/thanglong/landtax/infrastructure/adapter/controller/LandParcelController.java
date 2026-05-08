package com.thanglong.landtax.infrastructure.adapter.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.thanglong.landtax.usecase.service.LandParcelService;
import com.thanglong.landtax.usecase.service.LandParcelImportService;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST Controller cho quản lý thửa đất.
 *
 * <p><b>Lưu ý:</b> Hiện tại trả về Mock Data để Frontend có thể test API.
 * Sẽ được kết nối với use case thực tế trong sprint tiếp theo.</p>
 */
@RestController
@RequestMapping("/api/land-parcels")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("all")
public class LandParcelController {

    private final LandParcelService landParcelService;
    private final LandParcelImportService landParcelImportService;
    private final com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandParcelJpaRepository landParcelJpaRepository;

    // --- Mock data dùng chung ---

    private static Map<String, Object> buildMockParcel(long id) {
        return Map.ofEntries(
                Map.entry("landParcelId",       id),
                Map.entry("parcelNumber",        "BD-" + String.format("%05d", id)),
                Map.entry("mapSheetNumber",      "F-48-68-C"),
                Map.entry("landTypeId",          1),
                Map.entry("landTypeName",        "Đất ở đô thị"),
                Map.entry("areaId",              2),
                Map.entry("areaName",            "Quận Hoàn Kiếm, Hà Nội"),
                Map.entry("areaSize",            new BigDecimal("120.50")),
                Map.entry("address",             String.format("Số %d, Phố Tràng Tiền, Hoàn Kiếm, Hà Nội", id * 5)),
                Map.entry("usageType",           "Nhà ở"),
                Map.entry("usageDuration",       "Lâu dài"),
                Map.entry("usageOrigin",         "Cấp mới"),
                Map.entry("certificateNumber",   "CT-" + String.format("%06d", id)),
                Map.entry("gcnBookNumber",       "SO-" + String.format("%03d", id)),
                Map.entry("attachedHouse",       "Nhà 3 tầng, diện tích xây dựng 80m²"),
                Map.entry("notes",               "Thửa đất đã có sổ đỏ đầy đủ pháp lý"),
                Map.entry("owner_cccd",          id == 1 ? "001190000101" : "001190000999")
        );
    }

    /**
     * GET /api/land-parcels/my-assets — Lấy danh sách thửa đất của riêng công dân (dựa vào JWT).
     */
    @GetMapping("/my-assets")
    @PreAuthorize("hasRole('CITIZEN')")
    public ResponseEntity<?> getMyLandParcels() {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("GET /api/land-parcels/my-assets — owner_cccd={}", cccd);
        
        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity> myParcels = landParcelService.getMyLandParcels(cccd);
        
        return ResponseEntity.ok(Map.of(
                "data", myParcels,
                "message", myParcels.isEmpty() ? "Không tìm thấy thửa đất nào" : "Lấy danh sách thửa đất thành công"
        ));
    }

    /**
     * POST /api/land-parcels/import — Import hàng loạt thửa đất từ file Excel.
     * Chỉ ROLE_LAND_OFFICER mới được phép.
     */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Import Excel thửa đất")
    public ResponseEntity<?> importLandParcels(@RequestParam("file") MultipartFile file) {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isLandOfficer = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("LAND_OFFICER") || a.getAuthority().contains("ADMIN"));

        if (!isLandOfficer) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden",
                    "message", "Chỉ LAND_OFFICER mới có quyền import dữ liệu thửa đất"
            ));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File không được để trống"));
        }

        String filename = java.util.Optional.ofNullable(file.getOriginalFilename()).orElse("");
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chỉ chấp nhận file Excel (.xlsx, .xls)"));
        }

        try {
            int count = landParcelImportService.importFromExcel(file);
            log.info("LAND_OFFICER {} import {} thửa đất từ Excel", cccd, count);
            return ResponseEntity.ok(Map.of(
                    "message", "Import thành công",
                    "imported", count
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Lỗi import Excel: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Lỗi xử lý file Excel: " + e.getMessage()));
        }
    }

    /**
     * GET /api/land-parcels/search — Tra cứu thửa đất theo tờ bản đồ và số thửa.
     * Chỉ trả dữ liệu nếu người tra cứu là chủ sở hữu hoặc cán bộ chuyên trách.
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchParcels(
            @RequestParam(required = false) String mapSheet,
            @RequestParam(required = false) String parcelNumber) {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isOfficer = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN") || a.getAuthority().contains("OFFICER"));

        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity> results;
        if (mapSheet != null && parcelNumber != null) {
            results = landParcelJpaRepository.findByMapSheetNumberAndParcelNumber(mapSheet, parcelNumber);
        } else if (mapSheet != null) {
            results = landParcelJpaRepository.findByMapSheetNumber(mapSheet);
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Cần ít nhất tham số mapSheet"));
        }

        // Nếu không phải cán bộ, chỉ trả về thửa đất do mình sở hữu
        if (!isOfficer) {
            final String finalCccd = cccd;
            results = results.stream()
                    .filter(p -> finalCccd.equals(p.getOwnerCccd()))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (results.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "data", List.of(),
                    "message", "Không tìm thấy thửa đất phù hợp hoặc bạn không có quyền xem"
            ));
        }

        return ResponseEntity.ok(Map.of("data", results, "total", results.size()));
    }

    /**
     * GET /api/land-parcels/all — Lấy toàn bộ danh sách thửa đất (không lọc theo chủ sở hữu).
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('LAND_OFFICER')")
    public ResponseEntity<?> getAllParcelsForOfficer() {
        log.info("GET /api/land-parcels/all — LAND_OFFICER xem toàn bộ thửa đất");
        List<com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandParcelEntity> allParcels = landParcelJpaRepository.findAll();
        
        return ResponseEntity.ok(Map.of(
                "data", allParcels,
                "total", allParcels.size(),
                "message", "Lấy danh sách toàn bộ thửa đất thành công"
        ));
    }

    /**
     * GET /api/land-parcels — Lấy danh sách thửa đất (phân trang).
     */
    @GetMapping
    @PreAuthorize("hasRole('LAND_OFFICER')")
    public ResponseEntity<?> getAllParcels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("[MOCK] GET /api/land-parcels — page={}, size={}", page, size);

        List<Map<String, Object>> mockList = List.of(
                buildMockParcel(1L),
                buildMockParcel(2L),
                buildMockParcel(3L)
        );

        return ResponseEntity.ok(Map.of(
                "content",       mockList,
                "totalElements", 3,
                "totalPages",    1,
                "page",          page,
                "size",          size,
                "message",       "[MOCK DATA] Danh sách thửa đất"
        ));
    }

    /**
     * GET /api/land-parcels/{id} — Lấy chi tiết một thửa đất.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getParcelById(@PathVariable Long id) {
        String cccd = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        boolean isAdmin = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("ADMIN"));

        Map<String, Object> mockParcel = buildMockParcel(id);
        String ownerCccd = (String) mockParcel.get("owner_cccd");

        if (!isAdmin && !cccd.equals(ownerCccd)) {
            log.warn("Access denied for CCCD: {} to parcel id: {}", cccd, id);
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Forbidden",
                    "message", "Bạn không có quyền xem thông tin thửa đất này"
            ));
        }

        log.info("[MOCK] GET /api/land-parcels/{}", id);

        return ResponseEntity.ok(Map.of(
                "data",    mockParcel,
                "message", "[MOCK DATA] Chi tiết thửa đất #" + id
        ));
    }

    /**
     * POST /api/land-parcels — Tạo mới thửa đất.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> createParcel(@RequestBody Map<String, Object> body) {
        log.info("[MOCK] POST /api/land-parcels — body keys: {}", body.keySet());

        Map<String, Object> created = Map.ofEntries(
                Map.entry("landParcelId",       99L),
                Map.entry("parcelNumber",        body.getOrDefault("parcelNumber", "BD-00099")),
                Map.entry("mapSheetNumber",      body.getOrDefault("mapSheetNumber", "F-48-68-D")),
                Map.entry("landTypeId",          body.getOrDefault("landTypeId", 1)),
                Map.entry("areaId",              body.getOrDefault("areaId", 1)),
                Map.entry("areaSize",            body.getOrDefault("areaSize", "100.00")),
                Map.entry("address",             body.getOrDefault("address", "Số 1, Đường ABC, Hà Nội")),
                Map.entry("usageType",           body.getOrDefault("usageType", "Nhà ở")),
                Map.entry("usageDuration",       "Lâu dài"),
                Map.entry("certificateNumber",   "CT-000099"),
                Map.entry("notes",               "Vừa được tạo mới")
        );

        return ResponseEntity.ok(Map.of(
                "data",    created,
                "message", "[MOCK DATA] Tạo thửa đất thành công"
        ));
    }

    /**
     * PUT /api/land-parcels/{id} — Cập nhật thông tin thửa đất.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> updateParcel(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        log.info("[MOCK] PUT /api/land-parcels/{} — body keys: {}", id, body.keySet());

        Map<String, Object> updated = Map.ofEntries(
                Map.entry("landParcelId",        id),
                Map.entry("parcelNumber",        body.getOrDefault("parcelNumber", "BD-" + String.format("%05d", id))),
                Map.entry("mapSheetNumber",      body.getOrDefault("mapSheetNumber", "F-48-68-C")),
                Map.entry("landTypeId",          body.getOrDefault("landTypeId", 1)),
                Map.entry("areaId",              body.getOrDefault("areaId", 2)),
                Map.entry("areaSize",            body.getOrDefault("areaSize", "120.50")),
                Map.entry("address",             body.getOrDefault("address", "Số " + (id * 5) + ", Hoàn Kiếm, Hà Nội")),
                Map.entry("usageType",           body.getOrDefault("usageType", "Nhà ở")),
                Map.entry("usageDuration",       "Lâu dài"),
                Map.entry("certificateNumber",   body.getOrDefault("certificateNumber", "CT-" + String.format("%06d", id))),
                Map.entry("notes",               body.getOrDefault("notes", "Đã được cập nhật"))
        );

        return ResponseEntity.ok(Map.of(
                "data",    updated,
                "message", "[MOCK DATA] Cập nhật thửa đất #" + id + " thành công"
        ));
    }

    /**
     * DELETE /api/land-parcels/{id} — Xóa thửa đất.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Xóa thửa đất")
    public ResponseEntity<?> deleteParcel(@PathVariable Long id) {
        log.info("[MOCK] DELETE /api/land-parcels/{}", id);

        return ResponseEntity.ok(Map.of(
                "deletedId", id,
                "message",   "[MOCK DATA] Xóa thửa đất #" + id + " thành công"
        ));
    }
}

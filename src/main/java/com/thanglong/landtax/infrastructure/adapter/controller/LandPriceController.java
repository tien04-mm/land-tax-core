package com.thanglong.landtax.infrastructure.adapter.controller;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.LandPriceEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AreaJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandPriceJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.LandTypeJpaRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.thanglong.landtax.usecase.service.AuditLogService;
import com.thanglong.landtax.usecase.service.LandPriceService;
import com.thanglong.landtax.usecase.dto.LandPriceHistoryResponseDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Land Price Controller quản lý bảng giá đất.
 *
 * <p>
 * Cho phép Cán bộ địa chính & Admin CRUD. Cán bộ thuế & Người dân đọc (GET).
 * </p>
 */
@RestController
@RequestMapping("/api/land-prices")
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class LandPriceController {

    private final LandPriceJpaRepository landPriceJpaRepository;
    private final LandTypeJpaRepository landTypeJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final AuditLogService auditLogService;
    private final LandPriceService landPriceService;

    @Data
    static class LandPriceRequest {
        @NotNull(message = "landTypeId không được để trống")
        private Integer landTypeId;

        @NotNull(message = "areaId không được để trống")
        private Integer areaId;

        @NotNull(message = "unitPrice không được để trống")
        @Positive(message = "unitPrice phải là số dương")
        private BigDecimal unitPrice;

        @NotNull(message = "appliedFrom không được để trống")
        private LocalDate appliedFrom;
    }

    /** Lấy lịch sử điều chỉnh đơn giá đất (sắp xếp theo ngày áp dụng giảm dần). */
    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER', 'TAX_OFFICER', 'CITIZEN')")
    public ResponseEntity<List<LandPriceHistoryResponseDTO>> getPriceHistory(
            @RequestParam(required = false) Integer landTypeId,
            @RequestParam(required = false) Integer areaId) {
        log.info("GET /api/land-prices/history - landTypeId={}, areaId={}", landTypeId, areaId);
        List<LandPriceHistoryResponseDTO> history = landPriceService.getPriceHistory(landTypeId, areaId);
        return ResponseEntity.ok(history);
    }

    /** Lấy toàn bộ bảng giá đất. Có thể lọc theo landTypeId hoặc areaId. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER', 'TAX_OFFICER', 'CITIZEN')")
    public ResponseEntity<?> getAllPrices(
            @RequestParam(required = false) Integer landTypeId,
            @RequestParam(required = false) Integer areaId) {

        log.info("GET /api/land-prices - landTypeId={}, areaId={}", landTypeId, areaId);

        List<LandPriceEntity> prices;
        if (landTypeId != null && areaId != null) {
            prices = landPriceJpaRepository.findByLandTypeIdAndAreaId(landTypeId, areaId);
        } else if (landTypeId != null) {
            prices = landPriceJpaRepository.findByLandTypeId(landTypeId);
        } else if (areaId != null) {
            prices = landPriceJpaRepository.findByAreaId(areaId);
        } else {
            prices = landPriceJpaRepository.findAll();
        }

        return ResponseEntity.ok(Map.of("data", prices, "total", prices.size()));
    }

    /** Lấy chi tiết một bản ghi gia đất theo ID. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER', 'TAX_OFFICER', 'CITIZEN')")
    public ResponseEntity<?> getPriceById(@PathVariable Integer id) {
        log.info("GET /api/land-prices/{}", id);
        return landPriceJpaRepository.findById(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of("data", p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Tra cứu đơn giá mới nhất theo loại đất và khu vực. (Công khai)
     */
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupLatestPrice(
            @RequestParam @NotNull Integer landTypeId,
            @RequestParam @NotNull Integer areaId) {

        log.info("GET /api/land-prices/lookup - landTypeId={}, areaId={}", landTypeId, areaId);

        return landPriceJpaRepository.findLatestPrice(landTypeId, areaId)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of(
                        "data", p,
                        "unitPrice", p.getUnitPrice(),
                        "appliedFrom", p.getAppliedFrom())))
                .orElse(ResponseEntity.ok(Map.of(
                        "data", null,
                        "message", "Không tìm thấy bảng giá cho loại đất và khu vực này")));
    }

    /** Tạo mới một bản ghi giá đất. Cho LAND_OFFICER / ADMIN. */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Tạo mới bảng giá đất")
    public ResponseEntity<?> createPrice(@Valid @RequestBody LandPriceRequest req) {
        log.info("POST /api/land-prices - landTypeId={}, areaId={}, unitPrice={}",
                req.getLandTypeId(), req.getAreaId(), req.getUnitPrice());

        // Kiểm tra FK hợp lệ
        if (!landTypeJpaRepository.existsById(req.getLandTypeId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "landTypeId không tồn tại"));
        }
        if (!areaJpaRepository.existsById(req.getAreaId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "areaId không tồn tại"));
        }

        LandPriceEntity entity = LandPriceEntity.builder()
                .landTypeId(req.getLandTypeId())
                .areaId(req.getAreaId())
                .unitPrice(req.getUnitPrice())
                .appliedFrom(req.getAppliedFrom())
                .build();

        LandPriceEntity saved = landPriceJpaRepository.save(entity);
        log.info("Đã tạo bản ghi giá đất priceId={}", saved.getPriceId());

        return ResponseEntity.ok(Map.of(
                "data", saved,
                "message", "Tạo bảng giá đất thành công"));
    }

    /** Cập nhật bảng giá đất theo vị trí và mục đích sử dụng. */
    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    public ResponseEntity<?> updatePriceByLocation(@Valid @RequestBody LandPriceRequest req) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("PUT /api/land-prices/update - landTypeId={}, areaId={}, unitPrice={}, by {}",
                req.getLandTypeId(), req.getAreaId(), req.getUnitPrice(), cccd);

        // Kiểm tra FK
        if (!landTypeJpaRepository.existsById(req.getLandTypeId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "landTypeId không tồn tại"));
        }
        var areaOpt = areaJpaRepository.findById(req.getAreaId());
        if (areaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "areaId không tồn tại"));
        }
        String streetName = areaOpt.get().getStreetName() != null ? areaOpt.get().getStreetName() : "khong ten";
        String areaName = "đường " + streetName + " (Vị trí " + areaOpt.get().getPositionLevel() + ")";

        // Tìm existing price
        List<LandPriceEntity> existingList = landPriceJpaRepository.findByLandTypeIdAndAreaId(req.getLandTypeId(),
                req.getAreaId());

        LandPriceEntity saved;
        if (!existingList.isEmpty()) {
            LandPriceEntity existing = existingList.get(0);
            existing.setUnitPrice(req.getUnitPrice());
            existing.setAppliedFrom(req.getAppliedFrom());
            saved = landPriceJpaRepository.save(existing);
        } else {
            LandPriceEntity entity = LandPriceEntity.builder()
                    .landTypeId(req.getLandTypeId())
                    .areaId(req.getAreaId())
                    .unitPrice(req.getUnitPrice())
                    .appliedFrom(req.getAppliedFrom())
                    .build();
            saved = landPriceJpaRepository.save(entity);
        }

        // Ghi AuditLog
        auditLogService.log("UPDATE_LAND_PRICE", "LAND_PRICE",
                String.valueOf(saved.getPriceId()),
                "Cán bộ địa chính " + cccd + " đã cập nhật bảng giá đất khu vực " + areaName);

        return ResponseEntity.ok(Map.of(
                "data", saved,
                "message", "Cập nhật bảng giá đất thành công"));
    }

    /** Cập nhật bản ghi giá đất theo ID. Cho LAND_OFFICER / ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Cập nhật bảng giá đất")
    public ResponseEntity<?> updatePrice(@PathVariable Integer id,
            @Valid @RequestBody LandPriceRequest req) {
        log.info("PUT /api/land-prices/{}", id);

        return landPriceJpaRepository.findById(id)
                .<ResponseEntity<?>>map(existing -> {
                    // Kiểm tra FK nếu có thay đổi
                    if (!landTypeJpaRepository.existsById(req.getLandTypeId())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "landTypeId không tồn tại"));
                    }
                    if (!areaJpaRepository.existsById(req.getAreaId())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "areaId không tồn tại"));
                    }

                    existing.setLandTypeId(req.getLandTypeId());
                    existing.setAreaId(req.getAreaId());
                    existing.setUnitPrice(req.getUnitPrice());
                    existing.setAppliedFrom(req.getAppliedFrom());

                    LandPriceEntity updated = landPriceJpaRepository.save(existing);
                    log.info("Đã cập nhật bản ghi giá đất priceId={}", id);
                    return ResponseEntity.ok(Map.of(
                            "data", updated,
                            "message", "Cập nhật bảng giá đất thành công"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Xóa bản ghi giá đất. Cho LAND_OFFICER / ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Xóa bảng giá đất")
    public ResponseEntity<?> deletePrice(@PathVariable Integer id) {
        log.info("DELETE /api/land-prices/{}", id);

        if (!landPriceJpaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        landPriceJpaRepository.deleteById(id);
        log.info("Đã xóa bản ghi giá đất priceId={}", id);

        return ResponseEntity.ok(Map.of(
                "deletedId", id,
                "message", "Xóa bảng giá đất thành công"));
    }
}

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
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Land Price Controller – CRUD bảng giá đất.
 *
 * <p>Dành cho Cán bộ địa chính (ROLE_LAND_OFFICER / ROLE_ADMIN) quản lý đơn giá đất.</p>
 *
 * <ul>
 *   <li>GET    /api/land-prices              – Lấy toàn bộ bảng giá</li>
 *   <li>GET    /api/land-prices/{id}         – Lấy chi tiết 1 bản ghi giá</li>
 *   <li>GET    /api/land-prices/lookup       – Tra cứu giá mới nhất theo loại đất + khu vực</li>
 *   <li>POST   /api/land-prices              – Tạo mới bản ghi giá đất</li>
 *   <li>PUT    /api/land-prices/{id}         – Cập nhật bản ghi giá đất</li>
 *   <li>DELETE /api/land-prices/{id}         – Xóa bản ghi giá đất</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/land-prices")
@RequiredArgsConstructor
@Slf4j
public class LandPriceController {

    private final LandPriceJpaRepository landPriceJpaRepository;
    private final LandTypeJpaRepository  landTypeJpaRepository;
    private final AreaJpaRepository      areaJpaRepository;

    // ──────────────────────────────────────────────────────────────────
    // DTO nội bộ
    // ──────────────────────────────────────────────────────────────────

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

    // ──────────────────────────────────────────────────────────────────
    // GET /api/land-prices
    // ──────────────────────────────────────────────────────────────────

    /** Lấy toàn bộ bảng giá đất. Có thể lọc theo landTypeId hoặc areaId. */
    @GetMapping
    public ResponseEntity<?> getAllPrices(
            @RequestParam(required = false) Integer landTypeId,
            @RequestParam(required = false) Integer areaId) {

        log.info("GET /api/land-prices — landTypeId={}, areaId={}", landTypeId, areaId);

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

    // ──────────────────────────────────────────────────────────────────
    // GET /api/land-prices/{id}
    // ──────────────────────────────────────────────────────────────────

    /** Lấy chi tiết một bản ghi giá đất theo ID. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPriceById(@PathVariable Integer id) {
        log.info("GET /api/land-prices/{}", id);
        return landPriceJpaRepository.findById(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of("data", p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/land-prices/lookup
    // ──────────────────────────────────────────────────────────────────

    /**
     * Tra cứu đơn giá mới nhất theo loại đất và khu vực.
     * Dùng để tính toán thuế tự động.
     */
    @GetMapping("/lookup")
    public ResponseEntity<?> lookupLatestPrice(
            @RequestParam @NotNull Integer landTypeId,
            @RequestParam @NotNull Integer areaId) {

        log.info("GET /api/land-prices/lookup — landTypeId={}, areaId={}", landTypeId, areaId);

        return landPriceJpaRepository.findLatestPrice(landTypeId, areaId)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of(
                        "data",      p,
                        "unitPrice", p.getUnitPrice(),
                        "appliedFrom", p.getAppliedFrom()
                )))
                .orElse(ResponseEntity.ok(Map.of(
                        "data",    null,
                        "message", "Không tìm thấy bảng giá cho loại đất và khu vực này"
                )));
    }

    // ──────────────────────────────────────────────────────────────────
    // POST /api/land-prices
    // ──────────────────────────────────────────────────────────────────

    /** Tạo mới một bản ghi giá đất. Chỉ LAND_OFFICER / ADMIN. */
    @PostMapping
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Tạo mới bảng giá đất")
    public ResponseEntity<?> createPrice(@Valid @RequestBody LandPriceRequest req) {
        log.info("POST /api/land-prices — landTypeId={}, areaId={}, unitPrice={}",
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
                "data",    saved,
                "message", "Tạo bảng giá đất thành công"
        ));
    }

    // ──────────────────────────────────────────────────────────────────
    // PUT /api/land-prices/{id}
    // ──────────────────────────────────────────────────────────────────

    /** Cập nhật bản ghi giá đất. Chỉ LAND_OFFICER / ADMIN. */
    @PutMapping("/{id}")
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
                            "data",    updated,
                            "message", "Cập nhật bảng giá đất thành công"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ──────────────────────────────────────────────────────────────────
    // DELETE /api/land-prices/{id}
    // ──────────────────────────────────────────────────────────────────

    /** Xóa bản ghi giá đất. Chỉ ADMIN. */
    @DeleteMapping("/{id}")
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
                "message",   "Xóa bảng giá đất thành công"
        ));
    }
}

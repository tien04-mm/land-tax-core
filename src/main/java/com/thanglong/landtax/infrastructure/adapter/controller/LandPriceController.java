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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Land Price Controller a CRUD bang gia dat.
 *
 * <p>
 * D nh cho Can bo dia chAnh (ROLE_LAND_OFFICER / ROLE_ADMIN) quan ly don gia
 * dat.
 * </p>
 *
 * <ul>
 * <li>GET /api/land-prices a Lay to n bo bang gia</li>
 * <li>GET /api/land-prices/{id} a Lay chi tiat 1 ban ghi gia</li>
 * <li>GET /api/land-prices/lookup a Tra cou gia moi nhat theo loai dat + khu
 * voc</li>
 * <li>POST /api/land-prices a Tao moi ban ghi gia dat</li>
 * <li>PUT /api/land-prices/{id} a Cap nhat ban ghi gia dat</li>
 * <li>DELETE /api/land-prices/{id} a Xoa ban ghi gia dat</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class LandPriceController {

    private final LandPriceJpaRepository landPriceJpaRepository;
    private final LandTypeJpaRepository landTypeJpaRepository;
    private final AreaJpaRepository areaJpaRepository;
    private final AuditLogService auditLogService;

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // DTO noi bo
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    @Data
    static class LandPriceRequest {
        @NotNull(message = "landTypeId khAng duoc de trong")
        private Integer landTypeId;

        @NotNull(message = "areaId khAng duoc de trong")
        private Integer areaId;

        @NotNull(message = "unitPrice khAng duoc de trong")
        @Positive(message = "unitPrice phai l  so duong")
        private BigDecimal unitPrice;

        @NotNull(message = "appliedFrom khAng duoc de trong")
        private LocalDate appliedFrom;
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // GET /api/land-prices
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Lay to n bo bang gia dat. Co the loc theo landTypeId hoac areaId. */
    @GetMapping("/api/land-prices")
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

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // GET /api/land-prices/{id}
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Lay chi tiat mot ban ghi gia dat theo ID. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPriceById(@PathVariable Integer id) {
        log.info("GET /api/land-prices/{}", id);
        return landPriceJpaRepository.findById(id)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of("data", p)))
                .orElse(ResponseEntity.notFound().build());
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // GET /api/land-prices/lookup
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /**
     * Tra cou don gia moi nhat theo loai dat v  khu voc.
     * DAng de tAnh toan thua to dong.
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
                        "message", "KhAng tim thay bang gia cho loai dat v  khu voc n y")));
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // POST /api/land-prices
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Tao moi mot ban ghi gia dat. Cho LAND_OFFICER / ADMIN. */
    @PostMapping("/api/land-prices")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Tao moi bang gia dat")
    public ResponseEntity<?> createPrice(@Valid @RequestBody LandPriceRequest req) {
        log.info("POST /api/land-prices - landTypeId={}, areaId={}, unitPrice={}",
                req.getLandTypeId(), req.getAreaId(), req.getUnitPrice());

        // Kiem tra FK hop le
        if (!landTypeJpaRepository.existsById(req.getLandTypeId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "landTypeId khAng ton tai"));
        }
        if (!areaJpaRepository.existsById(req.getAreaId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "areaId khAng ton tai"));
        }

        LandPriceEntity entity = LandPriceEntity.builder()
                .landTypeId(req.getLandTypeId())
                .areaId(req.getAreaId())
                .unitPrice(req.getUnitPrice())
                .appliedFrom(req.getAppliedFrom())
                .build();

        LandPriceEntity saved = landPriceJpaRepository.save(entity);
        log.info("A tao ban ghi gia dat priceId={}", saved.getPriceId());

        return ResponseEntity.ok(Map.of(
                "data", saved,
                "message", "Tao bang gia dat th nh cAng"));
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // PUT /api/land-prices/update
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Cap nhat bang gia dat theo vi trA v  moc dAch so dong. */
    @PutMapping("/update")
    @PreAuthorize("hasRole('LAND_OFFICER')")
    public ResponseEntity<?> updatePriceByLocation(@Valid @RequestBody LandPriceRequest req) {
        String cccd = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("PUT /api/land-prices/update - landTypeId={}, areaId={}, unitPrice={}, by {}",
                req.getLandTypeId(), req.getAreaId(), req.getUnitPrice(), cccd);

        // Kiem tra FK
        if (!landTypeJpaRepository.existsById(req.getLandTypeId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "landTypeId khAng ton tai"));
        }
        var areaOpt = areaJpaRepository.findById(req.getAreaId());
        if (areaOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "areaId khAng ton tai"));
        }
        String streetName = areaOpt.get().getStreetName() != null ? areaOpt.get().getStreetName() : "khong ten";
        String areaName = "duong " + streetName + " (Vi tri " + areaOpt.get().getPositionLevel() + ")";

        // Tim existing price
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
                "Can bo dia chAnh " + cccd + " dA cap nhat bang gia dat khu voc " + areaName);

        return ResponseEntity.ok(Map.of(
                "data", saved,
                "message", "Cap nhat bang gia dat th nh cAng"));
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // PUT /api/land-prices/{id}
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Cap nhat ban ghi gia dat. Cho LAND_OFFICER / ADMIN. */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LAND_OFFICER')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Cap nhat bang gia dat")
    public ResponseEntity<?> updatePrice(@PathVariable Integer id,
            @Valid @RequestBody LandPriceRequest req) {
        log.info("PUT /api/land-prices/{}", id);

        return landPriceJpaRepository.findById(id)
                .<ResponseEntity<?>>map(existing -> {
                    // Kiem tra FK nau co thay doi
                    if (!landTypeJpaRepository.existsById(req.getLandTypeId())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "landTypeId khAng ton tai"));
                    }
                    if (!areaJpaRepository.existsById(req.getAreaId())) {
                        return ResponseEntity.badRequest().body(Map.of("error", "areaId khAng ton tai"));
                    }

                    existing.setLandTypeId(req.getLandTypeId());
                    existing.setAreaId(req.getAreaId());
                    existing.setUnitPrice(req.getUnitPrice());
                    existing.setAppliedFrom(req.getAppliedFrom());

                    LandPriceEntity updated = landPriceJpaRepository.save(existing);
                    log.info("A cap nhat ban ghi gia dat priceId={}", id);
                    return ResponseEntity.ok(Map.of(
                            "data", updated,
                            "message", "Cap nhat bang gia dat th nh cAng"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    // DELETE /api/land-prices/{id}
    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa

    /** Xoa ban ghi gia dat. Cho ADMIN. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @com.thanglong.landtax.infrastructure.config.aop.AuditLog(action = "Xoa bang gia dat")
    public ResponseEntity<?> deletePrice(@PathVariable Integer id) {
        log.info("DELETE /api/land-prices/{}", id);

        if (!landPriceJpaRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        landPriceJpaRepository.deleteById(id);
        log.info("A xoa ban ghi gia dat priceId={}", id);

        return ResponseEntity.ok(Map.of(
                "deletedId", id,
                "message", "Xoa bang gia dat th nh cAng"));
    }
}

